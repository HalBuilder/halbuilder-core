package com.theoryinpractise.halbuilder;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.theoryinpractise.halbuilder.bytecode.InterfaceContract;
import com.theoryinpractise.halbuilder.bytecode.InterfaceRenderer;
import com.theoryinpractise.halbuilder.json.JsonRenderer;
import com.theoryinpractise.halbuilder.xml.XmlRenderer;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;

import static java.lang.String.format;

public class MutableResource implements Resource {

    protected String href;
    protected Map<String, String> namespaces = Maps.newTreeMap(Ordering.usingToString());
    protected Multimap<String, String> links = ArrayListMultimap.create();
    protected Map<String, Object> properties = Maps.newTreeMap(Ordering.usingToString());
    protected Multimap<String, ReadableResource> resources = ArrayListMultimap.create();
    private ResourceFactory resourceFactory;

    public MutableResource(ResourceFactory resourceFactory, String href) {
        this.resourceFactory = resourceFactory;
        this.href = href;
    }

    public MutableResource withLink(String rel, String url) {
        links.put(rel, url);
        return this;
    }

    public Resource withProperty(String name, Object value) {
        if (properties.containsKey(name)) {
            throw new ResourceException(format("Duplicate property '%s' found for resource %s", name, href));
        }
        properties.put(name, value);
        return this;
    }

    public Resource withBean(Object value) {
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

    public Resource withFields(Object value) {
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

    public Resource withFieldBasedSubresource(String rel, String href, Object o) {
        return withSubresource(rel, resourceFactory.newHalResource(href).withFields(o));
    }

    public Resource withBeanBasedSubresource(String rel, String href, Object o) {
        return withSubresource(rel, resourceFactory.newHalResource(href).withBean(o));
    }

    public Resource withNamespace(String namespace, String url) {
        if (namespaces.containsKey(namespace)) {
            throw new ResourceException(format("Duplicate namespace '%s' found for resource %s", namespace, href));
        }
        namespaces.put(namespace, url);
        return this;
    }

    public MutableResource withSubresource(String rel, ReadableResource resource) {
        resources.put(rel, resource);
        return this;
    }

    public String getHref() {
        return href;
    }

    public Map<String, String> getNamespaces() {
        return ImmutableMap.copyOf(namespaces);
    }

    public Multimap<String, String> getLinks() {
        return ImmutableMultimap.copyOf(links);
    }

    public Map<String, Object> getProperties() {
        return ImmutableMap.copyOf(properties);
    }

    public Multimap<String, ReadableResource> getResources() {
        return ImmutableMultimap.copyOf(resources);
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
            throw new ResourceException(e.getMessage());
        }
    }

    public Resource withHref(String href) {
        this.href = href;
        return this;
    }

    public Resource withBaseHref(String baseHref) {
        try {
            this.href = new URL(new URL(baseHref), this.href).toExternalForm();
        } catch (MalformedURLException e) {
            throw new ResourceException(e.getMessage());
        }
        return this;
    }

    private void validateNamespaces(ReadableResource resource) {
        for (String rel : resource.getLinks().keySet()) {
            validateNamespaces(resource.getHref(), rel);
        }
        for (Map.Entry<String, Collection<ReadableResource>> entry : resource.getResources().asMap().entrySet()) {
            for (ReadableResource halResource : entry.getValue()) {
                validateNamespaces(resource.getHref(), entry.getKey());
                validateNamespaces(halResource);
            }
        }
    }

    private void validateNamespaces(String href, String sourceRel) {
        for (String rel : sourceRel.split(" ")) {
            if (rel.contains(":")) {
                String[] relPart = rel.split(":");
                if (!namespaces.keySet().contains(relPart[0])) {
                    throw new ResourceException(format("Undeclared namespace in rel %s for resource %s", rel, href));
                }
            }
        }
    }

    /**
     * Renders the current Resource as a proxy to the provider interface
     *
     * @param anInterface The interface we wish to proxy the resource as
     * @return A Guava Optional of the rendered class, this will be absent if the interface doesn't satisfy the interface
     */
    public <T> Optional<T> renderClass(Class<T> anInterface) {
        if (InterfaceContract.newInterfaceContract(anInterface).isSatisfiedBy(this)) {
            return InterfaceRenderer.createInterfaceRenderer(anInterface).render(this, null);
        } else {
            return Optional.absent();
        }
    }

    public String renderJson() {
        return renderJson(new JsonRenderer());
    }

    public String renderXml() {
        return renderJson(new XmlRenderer());
    }


    private String renderJson(final Renderer renderer) {
        validateNamespaces(this);

        StringWriter sw = new StringWriter();
        renderer.render(this, sw);
        return sw.toString();
    }

    /**
     * Test whether the Resource in its current state satisfies the provided interface.
     *
     * @param anInterface The interface we wish to check
     * @return Is that Resource structurally like the interface?
     */
    public <T> boolean isSatisfiedBy(Contract contract) {
        return contract.isSatisfiedBy(this);
    }

    public <T, V> Optional<V> ifSatisfiedBy(Class<T> anInterface, Function<T, V> function) {
        if (InterfaceContract.newInterfaceContract(anInterface).isSatisfiedBy(this)) {
            Optional<T> proxy = InterfaceRenderer.createInterfaceRenderer(anInterface).render(this, null);
            if (proxy.isPresent()) {
                return Optional.of(function.apply(proxy.get()));
            }
        }
        return Optional.absent();
    }

    public ReadableResource asImmutableResource() {
        return new ImmutableResource(getHref(), getNamespaces(), getLinks(), getProperties(), getResources());
    }
}
