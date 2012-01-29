package com.theoryinpractise.halbuilder.resources;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Table;
import com.theoryinpractise.halbuilder.Contract;
import com.theoryinpractise.halbuilder.Link;
import com.theoryinpractise.halbuilder.ReadableResource;
import com.theoryinpractise.halbuilder.Relatable;
import com.theoryinpractise.halbuilder.Renderer;
import com.theoryinpractise.halbuilder.Resource;
import com.theoryinpractise.halbuilder.ResourceException;
import com.theoryinpractise.halbuilder.ResourceFactory;
import com.theoryinpractise.halbuilder.bytecode.InterfaceContract;
import com.theoryinpractise.halbuilder.bytecode.InterfaceRenderer;
import com.theoryinpractise.halbuilder.json.JsonRenderer;
import com.theoryinpractise.halbuilder.xml.XmlRenderer;

import javax.annotation.Nullable;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static java.lang.String.format;

public class MutableResource implements Resource {

    public static final Ordering<Relatable> RELATABLE_ORDERING = Ordering.from(new Comparator<Relatable>() {
        public int compare(Relatable l1, Relatable l2) {
            if (l1.getRel().contains("self")) return -1;
            if (l2.getRel().contains("self")) return 1;
            return l1.getRel().compareTo(l2.getRel());
        }
    });

    protected Map<String, String> namespaces = Maps.newTreeMap(Ordering.usingToString());
    protected List<Link> links = Lists.newArrayList();
    protected Map<String, Object> properties = Maps.newTreeMap(Ordering.usingToString());
    protected List<Resource> resources = Lists.newArrayList();
    private ResourceFactory resourceFactory;

    public MutableResource(ResourceFactory resourceFactory, String href) {
        this.resourceFactory = resourceFactory;
        this.links.add(new Link(resolveRelativeHref(resourceFactory.getBaseHref(), href), "self"));
    }

    public MutableResource(ResourceFactory resourceFactory) {
        this.resourceFactory = resourceFactory;
    }

    public MutableResource(ResourceFactory resourceFactory, Link selfLink) {
        this.resourceFactory = resourceFactory;
        this.withLink(selfLink);
    }

    public Link getSelfLink() {
        try {
            return Iterables.find(getLinks(), new SelfLinkPredicate());
        } catch (NoSuchElementException e) {
            throw new IllegalStateException("Resources MUST have a self link.");
        }
    }

    public String getHref() {
        return getSelfLink().getHref();
    }

    public String getRel() {
        return getSelfLink().getRel();
    }

    public MutableResource withLink(String href, String rel) {
        String resolvedHref = resolveRelativeHref(href);
        for (String reltype : Splitter.on(" ").split(rel)) {
            links.add(new Link(resolvedHref, reltype));
        }

        return this;
    }

    public MutableResource withLink(Link link) {
        for (String reltype : Splitter.on(" ").split(link.getRel())) {
            links.add(new Link(link.getHref(), reltype));
        }

        return this;
    }

    public Resource withProperty(String name, Object value) {
        if (properties.containsKey(name)) {
            throw new ResourceException(format("Duplicate property '%s' found for resource", name));
        }
        if (value != null) {
            properties.put(name, value);
        }
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

    public Resource withNamespace(String namespace, String href) {
        if (namespaces.containsKey(namespace)) {
            throw new ResourceException(format("Duplicate namespace '%s' found for resource", namespace));
        }
        namespaces.put(namespace, resolveRelativeHref(href));
        return this;
    }

    public MutableResource withSubresource(String rel, Resource resource) {
        resource.withLink(resource.getSelfLink().getHref(), rel);
        resources.add(resource);
        return this;
    }

    public Map<String, String> getNamespaces() {
        return ImmutableMap.copyOf(namespaces);
    }

    public List<Link> getCanonicalLinks() {
        return ImmutableList.copyOf(links);
    }

    public List<Link> getLinks() {
        List<Link> collatedLinks = Lists.newArrayList();

        // href, rel, link
        Table<String, String, Link> linkTable = HashBasedTable.create();

        for (Link link : links) {
            linkTable.put(link.getHref(), link.getRel(), link);
        }

        for (String href : linkTable.rowKeySet()) {
            Map<String, Link> linkRelMap = linkTable.row(href);
            String rels = Joiner.on(" ").join(Ordering.usingToString().sortedCopy(linkRelMap.keySet()));
            collatedLinks.add(new Link(href, rels));
        }

        return RELATABLE_ORDERING.sortedCopy(collatedLinks);
    }

    public List<Link> getLinksByRel(final String rel) {
        return ImmutableList.copyOf(Iterables.filter(getLinks(), new Predicate<Relatable>() {
            public boolean apply(@Nullable Relatable relatable) {
                return Iterables.contains(Splitter.on(" ").split(relatable.getRel()), rel);
            }
        }));
    }

    public Map<String, Object> getProperties() {
        return ImmutableMap.copyOf(properties);
    }

    public List<Resource> getResources() {
        return ImmutableList.copyOf(resources);
    }

    private void validateNamespaces(ReadableResource resource) {
        for (Relatable link : resource.getCanonicalLinks()) {
            validateNamespaces(link.getRel());
        }
        for (Resource aResource : resource.getResources()) {
            validateNamespaces(aResource);
        }
    }

    private void validateNamespaces(String sourceRel) {
        for (String rel : Splitter.on(" ").split(sourceRel)) {
            if (!rel.contains("://") && rel.contains(":")) {
                String[] relPart = rel.split(":");
                if (!namespaces.keySet().contains(relPart[0])) {
                    throw new ResourceException(format("Undeclared namespace in rel %s for resource", rel));
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
            return InterfaceRenderer.newInterfaceRenderer(anInterface).render(this, null);
        } else {
            return Optional.absent();
        }
    }

    public String renderJson() {
        return renderAsString(new JsonRenderer());
    }

    public String renderXml() {
        return renderAsString(new XmlRenderer());
    }

    private String renderAsString(final Renderer renderer) {
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
            Optional<T> proxy = InterfaceRenderer.newInterfaceRenderer(anInterface).render(this, null);
            if (proxy.isPresent()) {
                return Optional.of(function.apply(proxy.get()));
            }
        }
        return Optional.absent();
    }

    public String resolveRelativeHref(String href) {
        return resolveRelativeHref(getSelfLink().getHref(), href);
    }

    private String resolveRelativeHref(final String baseHref, String href) {

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


    public ReadableResource asImmutableResource() {
        return new ImmutableResource(resourceFactory, getNamespaces(), getCanonicalLinks(), getProperties(), getResources());
    }

    private static class SelfLinkPredicate implements Predicate<Relatable> {
        public boolean apply(@Nullable Relatable relatable) {
            return relatable.getRel().contains("self");
        }
    }
}
