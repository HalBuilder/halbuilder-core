package com.theoryinpractise.halbuilder.impl.representations;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.theoryinpractise.halbuilder.RepresentationFactory;
import com.theoryinpractise.halbuilder.impl.api.Support;
import com.theoryinpractise.halbuilder.spi.Link;
import com.theoryinpractise.halbuilder.spi.ReadableRepresentation;
import com.theoryinpractise.halbuilder.spi.Representation;
import com.theoryinpractise.halbuilder.spi.RepresentationException;
import com.theoryinpractise.halbuilder.spi.Serializable;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URI;

import static com.theoryinpractise.halbuilder.impl.api.Support.WHITESPACE_SPLITTER;
import static java.lang.String.format;

public class MutableRepresentation extends BaseRepresentation implements Representation {

    public MutableRepresentation(RepresentationFactory representationFactory, String href) {
        super(representationFactory);
        this.links.add(new Link(representationFactory, resolveRelativeHref(representationFactory.getBaseHref(), href), "self"));
    }

    public MutableRepresentation(RepresentationFactory representationFactory) {
        super(representationFactory);
    }

    /**
     * Add a link to this resource
     *
     * @param rel
     * @param href The target href for the link, relative to the href of this resource.
     * @return
     */
    public MutableRepresentation withLink(String rel, String href) {
        withLink(rel, href,
                 Optional.of(Predicates.<ReadableRepresentation>alwaysTrue()),
                Optional.<String>absent(),
                Optional.<String>absent(),
                Optional.<String>absent());
        return this;
    }

    /**
     * Add a link to this resource
     *
     * @param rel
     * @param uri The target URI for the link, possibly relative to the href of
     *            this resource.
     * @return
     */
    public MutableRepresentation withLink(String rel, URI uri) {
    	return withLink(rel, uri.toASCIIString());
    }

    /**
     * Add a link to this resource
     *
     * @param rel
     * @param href The target href for the link, relative to the href of this resource.
     * @return
     */
    public MutableRepresentation withLink(String rel, String href, Predicate<ReadableRepresentation> predicate) {
        withLink(rel, href,
                 Optional.of(predicate),
                Optional.<String>absent(),
                Optional.<String>absent(),
                Optional.<String>absent());
        return this;
    }

    /**
     * Add a link to this resource
     *
     * @param rel
     * @param uri The target URI for the link, possibly relative to the href of
     *            this resource.
     * @return
     */
    public MutableRepresentation withLink(String rel, URI uri, Predicate<ReadableRepresentation> predicate) {
    	return withLink(rel, uri.toASCIIString(), predicate);
    }

	/**
	 * Add a link to this resource
	 *
     * @param rel
     * @param href The target href for the link, relative to the href of this resource.
     * @return
	 */
	public MutableRepresentation withLink(String rel, String href, Optional<Predicate<ReadableRepresentation>> predicate, Optional<String> name, Optional<String> title, Optional<String> hreflang) {

        Support.checkRelType(rel);

        if (predicate.or(Predicates.<ReadableRepresentation>alwaysTrue()).apply(this)) {
            String resolvedHref = resolvableUri.matcher(href).matches() ? resolveRelativeHref(href) : href;
            for (String reltype : WHITESPACE_SPLITTER.split(rel)) {
                String resolvedRelType = resolvableUri.matcher(reltype).matches() ? resolveRelativeHref(reltype) : reltype;
                links.add(new Link(representationFactory, resolvedHref, resolvedRelType, name, title, hreflang));
            }
        }

        return this;
    }

	/**
	 * Add a link to this resource
	 *
     * @param rel
     * @param uri The target URI for the link, possibly relative to the href of
     *            this resource.
     * @return
	 */
	public MutableRepresentation withLink(String rel, URI uri, Optional<Predicate<ReadableRepresentation>> predicate, Optional<String> name, Optional<String> title, Optional<String> hreflang) {
		return withLink(rel, uri.toASCIIString(), predicate, name, title, hreflang);
	}

    public Representation withProperty(String name, Object value) {
        if (properties.containsKey(name)) {
            throw new RepresentationException(format("Duplicate property '%s' found for resource", name));
        }
        if (null == value) {
            this.hasNullProperties = true;
        }
        properties.put(name, Optional.fromNullable(value));
        return this;
    }

    public Representation withBean(Object value) {
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

    public Representation withFields(Object value) {
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

    public Representation withSerializable(Serializable serializable) {
        serializable.serializeResource(this);
        return this;
    }

    public Representation withFieldBasedSubresource(String rel, String href, Object o) {
        return withSubresource(rel, representationFactory.newResource(href).withFields(o));
    }

    public Representation withBeanBasedSubresource(String rel, String href, Object o) {
        return withSubresource(rel, representationFactory.newResource(href).withBean(o));
    }

    /**
     * Adds a new namespace
     * @param namespace
     * @param href The target href of the namespace being added. This may be relative to the resourceFactories baseref
     * @return
     */
    public Representation withNamespace(String namespace, String href) {
        if (namespaces.containsKey(namespace)) {
            throw new RepresentationException(format("Duplicate namespace '%s' found for resource", namespace));
        }
        namespaces.put(namespace, resolveRelativeHref(representationFactory.getBaseHref(), href));
        return this;
    }

    public MutableRepresentation withSubresource(String rel, Representation resource) {
        Support.checkRelType(rel);
        resources.put(rel, resource);
        // Propagate null property flag to parent.
        if(resource.hasNullProperties()) {
            hasNullProperties = true;
        }
        return this;
    }

}
