package com.theoryinpractise.halbuilder.impl.resources;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Table;
import com.theoryinpractise.halbuilder.ResourceFactory;
import com.theoryinpractise.halbuilder.impl.api.Support;
import com.theoryinpractise.halbuilder.impl.bytecode.InterfaceContract;
import com.theoryinpractise.halbuilder.impl.bytecode.InterfaceRenderer;
import com.theoryinpractise.halbuilder.spi.Contract;
import com.theoryinpractise.halbuilder.spi.Link;
import com.theoryinpractise.halbuilder.spi.ReadableResource;
import com.theoryinpractise.halbuilder.spi.Resource;
import com.theoryinpractise.halbuilder.spi.ResourceException;

import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Pattern;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Ordering.usingToString;
import static com.theoryinpractise.halbuilder.impl.api.Support.WHITESPACE_SPLITTER;
import static com.theoryinpractise.halbuilder.impl.resources.LinkPredicate.newLinkPredicate;
import static java.lang.String.format;

public abstract class BaseResource implements ReadableResource {

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
    protected List<Resource> resources = Lists.newArrayList();
    protected ResourceFactory resourceFactory;
    protected final Pattern resolvableUri = Pattern.compile("^[/|?|~].*");

    protected BaseResource(ResourceFactory resourceFactory) {
        this.resourceFactory = resourceFactory;
    }

    public Link getResourceLink() {
        try {
            return Iterables.find(getLinks(), newLinkPredicate(Support.SELF));
        } catch (NoSuchElementException e) {
            throw new IllegalStateException("Resources MUST have a self link.");
        }
    }

    public Map<String, String> getNamespaces() {
        return ImmutableMap.copyOf(namespaces);
    }

    public List<Link> getCanonicalLinks() {
        return ImmutableList.copyOf(links);
    }

    public Optional<Link> getLinkByRel(String rel) {
        return fromNullable(Iterables.getFirst(getLinksByRel(rel), null));
    }

    public List<Link> getLinksByRel(final String rel) {
        Preconditions.checkArgument(rel != null, "Provided rel should not be null.");
        Preconditions.checkArgument(!"".equals(rel) && !rel.contains(" "), "Provided rel should not be empty or contain spaces.");

        final String resolvedRelType = resolvableUri.matcher(rel).matches() ? resolveRelativeHref(rel) : rel;
        final String curiedRel = currieHref(resolvedRelType);
        final ImmutableList.Builder<Link> linkBuilder = ImmutableList.builder();

        linkBuilder.addAll(getLinksByRel(this, curiedRel));
        for (Resource resource : resources) {
            linkBuilder.addAll(getLinksByRel(resource, curiedRel));
        }

        return linkBuilder.build();
    }

    public Optional<Object> get(String name) {
        return fromNullable(properties.get(name));
    }

    private List<Link> getLinksByRel(ReadableResource resource, final String curiedRel) {
        return ImmutableList.copyOf(Iterables.filter(resource.getLinks(), new Predicate<Link>() {
            public boolean apply(@Nullable Link relatable) {
                return Iterables.contains(WHITESPACE_SPLITTER.split(relatable.getRel()), curiedRel);
            }
        }));
    }

    public List<Link> getLinks() {
        List<Link> collatedLinks = Lists.newArrayList();

        // href, rel, link
        Table<String, String, Link> linkTable = HashBasedTable.create();

        for (Link link : links) {
            linkTable.put(link.getHref(), link.getRel(), link);
        }

        for (String href : linkTable.rowKeySet()) {
            Set<String> relTypes = linkTable.row(href).keySet();
            Collection<Link> hrefLinks = linkTable.row(href).values();

            // TODO I'm not sure I like this - when collating links we 'lose' the titles, names, and lang - so we
            // combine them - it feels iki tho.
            String rels = Joiner.on(" ").skipNulls().join(usingToString().sortedCopy(transform(relTypes, new Function<String, String>() {
                public String apply(@Nullable String relType) {
                    return currieHref(relType);
                }
            })));

            Ordering<Object> ordering = usingToString().nullsFirst();
            Joiner joiner = Joiner.on(", ").skipNulls();

            String titles = joiner.join(ordering.sortedCopy(transform(hrefLinks, new Function<Link, Object>() {
                public Object apply(@Nullable Link link) {
                    return link.getTitle().orNull();
                }
            })));

            String names = joiner.join(ordering.sortedCopy(transform(hrefLinks, new Function<Link, Object>() {
                public Object apply(@Nullable Link link) {
                    return link.getName().orNull();
                }
            })));

            String hreflangs = joiner.join(ordering.sortedCopy(transform(hrefLinks, new Function<Link, Object>() {
                public Object apply(@Nullable Link link) {
                    return link.getHreflang().orNull();
                }
            })));


            String curiedHref = currieHref(href);

            collatedLinks.add(new Link(resourceFactory, curiedHref, rels,
                                              fromNullable(emptyToNull(names)),
                                              fromNullable(emptyToNull(titles)),
                                              fromNullable(emptyToNull(hreflangs))));
        }

        return RELATABLE_ORDERING.sortedCopy(collatedLinks);
    }

    private String currieHref(String href) {
        if (href.contains("://")) {
            for (Map.Entry<String, String> entry : namespaces.entrySet()) {
                if (href.startsWith(entry.getValue())) {
                    return href.replace(entry.getValue(), entry.getKey() + ":");
                }
            }
        }
        return href;
    }

    public Map<String, Object> getProperties() {
        return ImmutableMap.copyOf(properties);
    }

    public List<Resource> getResources() {
        return ImmutableList.copyOf(resources);
    }

    protected  void validateNamespaces(ReadableResource resource) {
        for (Link link : resource.getCanonicalLinks()) {
            validateNamespaces(link.getRel());
        }
        for (Resource aResource : resource.getResources()) {
            validateNamespaces(aResource);
        }
    }

    private void validateNamespaces(String sourceRel) {
        for (String rel : WHITESPACE_SPLITTER.split(sourceRel)) {
            if (!rel.contains("://") && rel.contains(":")) {
                String[] relPart = rel.split(":");
                if (!namespaces.keySet().contains(relPart[0])) {
                    throw new ResourceException(format("Undeclared namespace in rel %s for resource", rel));
                }
            }
        }
    }

    /**
     * Test whether the Resource in its current state satisfies the provided interface.
     *
     * @param anInterface The interface we wish to check
     * @return Is that Resource structurally like the interface?
     */
    public boolean isSatisfiedBy(Contract contract) {
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
        return resolveRelativeHref(getResourceLink().getHref(), href);
    }

    protected String resolveRelativeHref(final String baseHref, String href) {

        try {
            if (href.startsWith("?")) {
                return new URL(baseHref + href).toExternalForm();
            } else if (href.startsWith("~/")) {
                if (baseHref.endsWith("/")) {
                    return new URL(baseHref + href.substring(2)).toExternalForm();
                } else {
                    return new URL(baseHref + href.substring(1)).toExternalForm();
                }
            } else {
                return new URL(new URL(baseHref), href).toExternalForm();
            }
        } catch (MalformedURLException e) {
            throw new ResourceException(e.getMessage());
        }

    }

    public ImmutableResource toImmutableResource() {
        return new ImmutableResource(resourceFactory, getNamespaces(), getCanonicalLinks(), getProperties(), getResources());
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
        if (!(obj instanceof BaseResource)) {
            return false;
        }
        BaseResource that = (BaseResource) obj;
        boolean e = this.namespaces.equals(that.namespaces);
        e &= this.links.equals(that.links);
        e &= this.properties.equals(that.properties);
        e &= this.resources.equals(that.resources);
        return e;
    }
    
    @Override
    public String toString() {
        Optional<Link> href = getLinkByRel("self");
        if (href.isPresent()) {
            return "<Resource: " + href.get().getHref() + ">";
        } else {
            return "<Resource: @" +  Integer.toHexString(hashCode()) + ">";
        }
    }
}
