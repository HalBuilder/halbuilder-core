package com.theoryinpractise.halbuilder;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.io.CharStreams;
import com.theoryinpractise.halbuilder.bytecode.InterfaceContract;
import com.theoryinpractise.halbuilder.bytecode.InterfaceRenderer;
import com.theoryinpractise.halbuilder.xml.XmlHalReader;
import com.theoryinpractise.halbuilder.json.JsonHalRenderer;
import com.theoryinpractise.halbuilder.xml.XmlHalRenderer;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import static java.lang.String.format;

public class HalResource {

    private String href;
    private TreeMap<String, String> namespaces = Maps.newTreeMap(Ordering.usingToString());
    private Multimap<String, String> links = ArrayListMultimap.create();
    private TreeMap<String, Object> properties = Maps.newTreeMap(Ordering.usingToString());
    private Multimap<String, HalResource> resources = ArrayListMultimap.create();

    private HalResource(String href) {
        this.href = href;
    }

    public static HalResource newHalResource(String href) {
        return new HalResource(href);
    }

    public static HalResource newHalResource(Reader reader) {
        String halSource = null;
        try {
            halSource = CharStreams.toString(reader);
            if (halSource.startsWith("<")) {
                return new XmlHalReader().read(halSource);
            }

            throw new IllegalArgumentException("Unknown resource format");
        } catch (IOException e) {
            throw new HalResourceException(e);
        }
    }

    public HalResource withLink(String rel, String url) {
        links.put(rel, url);
        return this;
    }

    public HalResource withProperty(String name, Object value) {
        if (properties.containsKey(name)) {
            throw new HalResourceException(format("Duplicate property '%s' found for resource %s", name, href));
        }
        properties.put(name, value);
        return this;
    }

    public HalResource withBean(Object value) {
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(value.getClass());
            for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
                if (!"class".equals(pd.getName())) {
                    withProperty(pd.getName(), pd.getReadMethod().invoke(value));
                }
            }

        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public HalResource withFields(Object value) {
        try {
            for (Field field : value.getClass().getDeclaredFields()) {
                if (Modifier.isPublic(field.getModifiers())) {
                    withProperty(field.getName(), field.get(value));
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return this;

    }

    public HalResource withFieldBasedSubresource(String rel, String href, Object o) {
        return withSubresource(rel, HalResource.newHalResource(href).withFields(o));
    }

    public HalResource withBeanBasedSubresource(String rel, String href, Object o) {
        return withSubresource(rel, HalResource.newHalResource(href).withBean(o));
    }

    public HalResource withNamespace(String namespace, String url) {
        if (namespaces.containsKey(namespace)) {
            throw new HalResourceException(format("Duplicate namespace '%s' found for resource %s", namespace, href));
        }
        namespaces.put(namespace, url);
        return this;
    }

    public HalResource withSubresource(String rel, HalResource resource) {
        resources.put(rel, resource);
        return this;
    }

    public String getHref() {
        return href;
    }

    public Map<String, String> getNamespaces() {
        return namespaces;
    }

    public Multimap<String, String> getLinks() {
        return links;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public Multimap<String, HalResource> getResources() {
        return resources;
    }

    public static String resolveRelativeHref(String baseHref, String href) {
        try {
            if (href.startsWith("?")) {
                return new URL(baseHref + href).toExternalForm();
            } else if (href.startsWith("~/")) {
                return new URL(baseHref + href.substring(1)).toExternalForm();
            } else {
                return new URL(new URL(baseHref), href).toExternalForm();
            }
        } catch (MalformedURLException e) {
            throw new HalResourceException(e.getMessage());
        }
    }

    public HalResource withHref(String href) {
        this.href = href;
        return this;
    }

    public HalResource withBaseHref(String baseHref) {
        try {
            this.href = new URL(new URL(baseHref), this.href).toExternalForm();
        } catch (MalformedURLException e) {
            throw new HalResourceException(e.getMessage());
        }
        return this;
    }

    private void validateNamespaces(HalResource resource) {
        for (String rel : resource.links.keySet()) {
            validateNamespaces(resource.href, rel);
        }
        for (Map.Entry<String, Collection<HalResource>> entry : resource.resources.asMap().entrySet()) {
            for (HalResource halResource : entry.getValue()) {
                validateNamespaces(resource.href, entry.getKey());
                validateNamespaces(halResource);
            }
        }
    }

    private void validateNamespaces(String href, String sourceRel) {
        for (String rel : sourceRel.split(" ")) {
            if (rel.contains(":")) {
                String[] relPart = rel.split(":");
                if (!namespaces.keySet().contains(relPart[0])) {
                    throw new HalResourceException(format("Undeclared namespace in rel %s for resource %s", rel, href));
                }
            }
        }
    }

    /**
     * Renders the current HalResource as a proxy to the provider interface
     *
     * @param anInterface The interface we wish to proxy the resource as
     * @return A Guava Optional of the rendered class, this will be absent if the interface doesn't satisfy the interface
     */
    public <T> Optional<T> renderClass(Class<T> anInterface) {
        if (InterfaceContract.createInterfaceContract(anInterface).isSatisfiedBy(this)) {
            return InterfaceRenderer.createInterfaceRenderer(anInterface).render(this, null);
        } else {
            return Optional.absent();
        }
    }

    public String renderJson() {
        return renderJson(new JsonHalRenderer());
    }

    public String renderXml() {
        return renderJson(new XmlHalRenderer());
    }


    private String renderJson(final HalRenderer renderer) {
        validateNamespaces(this);

        StringWriter sw = new StringWriter();
        renderer.render(this, sw);
        return sw.toString();
    }

    /**
     * Test whether the HalResource in its current state satisfies the provided interface.
     *
     * @param anInterface The interface we wish to check
     * @return Is that HalResource structurally like the interface?
     */
    public <T> boolean isSatisfiedBy(HalContract halContract) {
        return halContract.isSatisfiedBy(this);
    }

    public <T, V> Optional<V> ifSatisfiedBy(Class<T> anInterface, Function<T, V> function) {
        if (InterfaceContract.createInterfaceContract(anInterface).isSatisfiedBy(this)) {
            Optional<T> proxy = InterfaceRenderer.createInterfaceRenderer(anInterface).render(this, null);
            if (proxy.isPresent()) {
                return Optional.of(function.apply(proxy.get()));
            }
        }
        return Optional.absent();
    }

}
