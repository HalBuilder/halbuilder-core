package com.theoryinpractise.halbuilder.impl.representations;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Table;
import com.theoryinpractise.halbuilder.api.Contract;
import com.theoryinpractise.halbuilder.api.Link;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.Renderer;
import com.theoryinpractise.halbuilder.api.RepresentationException;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.impl.api.Support;
import com.theoryinpractise.halbuilder.impl.bytecode.InterfaceContract;
import com.theoryinpractise.halbuilder.impl.bytecode.InterfaceRenderer;

import javax.annotation.Nullable;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Ordering.usingToString;
import static com.theoryinpractise.halbuilder.impl.api.Support.WHITESPACE_SPLITTER;
import static java.lang.String.format;

public abstract class BaseRepresentation implements ReadableRepresentation {

    public static final Ordering<Link> RELATABLE_ORDERING = Ordering.from(new Comparator<Link>() {
        public int compare(Link l1, Link l2) {
            if (l1.getRel().contains("self")) return -1;
            if (l2.getRel().contains("self")) return 1;
            return l1.getRel().compareTo(l2.getRel());
        }
    });

    protected Map<String, String> namespaces = Maps.newTreeMap(usingToString());
    protected List<Link> links = Lists.newArrayList();
    protected Map<String, Object> properties = Maps.newTreeMap(usingToString());
    protected Multimap<String,ReadableRepresentation> resources = ArrayListMultimap.create();

    protected RepresentationFactory representationFactory;
    protected final Pattern resolvableUri = Pattern.compile("^[/|?|~].*");
    protected boolean hasNullProperties = false;

    protected BaseRepresentation(RepresentationFactory representationFactory) {
        this.representationFactory = representationFactory;
    }

    public Link getResourceLink() {
        return Iterables.find(getLinks(), LinkPredicate.newLinkPredicate(Support.SELF), null);
    }

    public Map<String, String> getNamespaces() {
        return ImmutableMap.copyOf(namespaces);
    }

    public List<Link> getCanonicalLinks() {
        return ImmutableList.copyOf(links);
    }

    public Link getLinkByRel(String rel) {
        return Iterables.getFirst(getLinksByRel(rel), null);
    }

    public List<Link> getLinksByRel(final String rel) {
        Preconditions.checkArgument(rel != null, "Provided rel should not be null.");
        Preconditions.checkArgument(!"".equals(rel) && !rel.contains(" "), "Provided rel should not be empty or contain spaces.");

        final String resolvedRelType = resolvableUri.matcher(rel).matches() ? resolveRelativeHref(rel) : rel;
        final String curiedRel = currieHref(resolvedRelType);
        final ImmutableList.Builder<Link> linkBuilder = ImmutableList.builder();

        linkBuilder.addAll(getLinksByRel(this, curiedRel));
        // TODO Should this check descendants? Should maybe be an overloaded method with a boolean check
        for (ReadableRepresentation resource : resources.values()) {
            linkBuilder.addAll(getLinksByRel(resource, curiedRel));
        }

        return linkBuilder.build();
    }

    public List<? extends ReadableRepresentation> getResourcesByRel(final String rel) {
        Support.checkRelType(rel);

        return ImmutableList.copyOf(resources.get(rel));
    }

    public List<? extends ReadableRepresentation> getResources(Predicate<ReadableRepresentation> predicate) {
        Preconditions.checkArgument(predicate != null, "Provided findPredicate should not be null.");
        return ImmutableList.copyOf(Iterables.filter(resources.values(), predicate));
    }

    public Object getValue(String name) {
        if (properties.containsKey(name)) {
            return properties.get(name);
        } else {
            throw new RepresentationException("Resource does not contain " + name);
        }
    }

    public Object getValue(String name, Object defaultValue) {
        try {
            return getValue(name);
        } catch (RepresentationException e) {
            return defaultValue;
        }
    }

    private List<Link> getLinksByRel(ReadableRepresentation representation, final String curiedRel) {
        Support.checkRelType(curiedRel);
        return ImmutableList.copyOf(Iterables.filter(representation.getLinks(), new Predicate<Link>() {
            public boolean apply(@Nullable Link relatable) {
                return Iterables.contains(WHITESPACE_SPLITTER.split(relatable.getRel()), curiedRel);
            }
        }));
    }

    public List<Link> getLinks() {
        return FluentIterable.from(links).transform(new Function<Link, Link>() {
            @Nullable
            @Override
            public Link apply(@Nullable Link link) {
                return new Link(representationFactory, currieHref(link.getHref()), currieHref(link.getRel()), link.getName(), link.getTitle(), link.getHreflang());
            }
        }).toSortedImmutableList(RELATABLE_ORDERING);

    }

    private String currieHref(String href) {
        for (Map.Entry<String, String> entry : namespaces.entrySet()) {
            if (href.startsWith(entry.getValue())) {
                return href.replace(entry.getValue(), entry.getKey() + ":");
            }
        }
        return href;
    }

    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public Collection<Map.Entry<String, ReadableRepresentation>> getResources() {
        return ImmutableMultimap.copyOf(resources).entries();
    }

    public Map<String, Collection<ReadableRepresentation>> getResourceMap() {
        return ImmutableMap.copyOf(resources.asMap());
    }

    protected  void validateNamespaces(ReadableRepresentation representation) {
        for (Link link : representation.getCanonicalLinks()) {
            validateNamespaces(link.getRel());
        }
        for (Map.Entry<String, ReadableRepresentation> aResource : representation.getResources()) {
            validateNamespaces(aResource.getKey());
            validateNamespaces(aResource.getValue());
        }
    }

    private void validateNamespaces(String sourceRel) {
        for (String rel : WHITESPACE_SPLITTER.split(sourceRel)) {
            if (!rel.contains("://") && rel.contains(":")) {
                String[] relPart = rel.split(":");
                if (!namespaces.keySet().contains(relPart[0])) {
                    throw new RepresentationException(format("Undeclared namespace in rel %s for resource", rel));
                }
            }
        }
    }

    /**
     * Test whether the Representation in its current state satisfies the provided interface.
     *
     * @param contract The interface we wish to check
     * @return Is that Representation satisfied by the supplied contract?
     */
    public boolean isSatisfiedBy(Contract contract) {
        return contract.isSatisfiedBy(this);
    }

    public String resolveRelativeHref(String href) {
        if (getResourceLink() != null) {
            return resolveRelativeHref(getResourceLink().getHref(), href);
        } else {
            throw new IllegalStateException("Unable to resolve relative href with missing resource href.");
        }
    }

    protected String resolveRelativeHref(final String baseHref, String href) {

        try {
            if (href.startsWith("?")) {
                return new URI(baseHref + href).toString();
            } else if (href.startsWith("~/")) {
                if (baseHref.endsWith("/")) {
                    return new URI(baseHref + href.substring(2)).toString();
                } else {
                    return new URI(baseHref + href.substring(1)).toString();
                }
            } else {
                return new URI(baseHref + "/" + href).toString();
            }
        } catch (URISyntaxException e) {
            throw new RepresentationException(e.getMessage());
        }

    }

    public boolean hasNullProperties() {
        return hasNullProperties;
    }

    public ImmutableRepresentation toImmutableResource() {
        return new ImmutableRepresentation(representationFactory, getNamespaces(), getCanonicalLinks(), getProperties(), getResources(), hasNullProperties);
    }


    /**
     * Renders the current Representation as a proxy to the provider interface
     *
     * @param anInterface The interface we wish to proxy the resource as
     * @return A Guava Optional of the rendered class, this will be absent if the interface doesn't satisfy the interface
     */
    public <T> T toClass(Class<T> anInterface) {
        if (InterfaceContract.newInterfaceContract(anInterface).isSatisfiedBy(this)) {
            return InterfaceRenderer.newInterfaceRenderer(anInterface).render(this);
        } else {
            throw new RepresentationException("Unable to render representation to " + anInterface.getName());
        }
    }

    public String toString(String contentType) {
        return toString(contentType, null);
    }

    public String toString(String contentType, final Set<URI> flags) {
        StringWriter sw = new StringWriter();
        toString(contentType, flags, sw);
        return sw.toString();
    }

    public void toString(String contentType, Set<URI> flags, Writer writer) {
        validateNamespaces(this);
        Renderer<String> renderer = representationFactory.lookupRenderer(contentType);
        ImmutableSet.Builder<URI> uriBuilder = ImmutableSet.<URI>builder().addAll(representationFactory.getFlags());
        if (flags != null) uriBuilder.addAll(flags);
        renderer.render(this, uriBuilder.build(), writer);
    }


    @Override
    public int hashCode() {
        int h = namespaces.hashCode();
        h += links.hashCode();
        h += properties.hashCode();
        h += resources.hashCode();
        return h;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof BaseRepresentation)) {
            return false;
        }
        BaseRepresentation that = (BaseRepresentation) obj;
        boolean e = this.namespaces.equals(that.namespaces);
        e &= this.links.equals(that.links);
        e &= this.properties.equals(that.properties);
        e &= this.resources.equals(that.resources);
        return e;
    }

    @Override
    public String toString() {
        Link href = getLinkByRel("self");
        if (href != null) {
            return "<Representation: " + href.getHref() + ">";
        } else {
            return "<Representation: @" +  Integer.toHexString(hashCode()) + ">";
        }
    }

}
