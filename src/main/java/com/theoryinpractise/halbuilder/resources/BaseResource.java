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
import com.theoryinpractise.halbuilder.RenderableResource;
import com.theoryinpractise.halbuilder.Resource;
import com.theoryinpractise.halbuilder.ResourceException;
import com.theoryinpractise.halbuilder.ResourceFactory;
import com.theoryinpractise.halbuilder.bytecode.InterfaceContract;
import com.theoryinpractise.halbuilder.bytecode.InterfaceRenderer;

import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Pattern;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Ordering.usingToString;
import static java.lang.String.format;

public abstract class BaseResource implements ReadableResource {

    public static final Ordering<Relatable> RELATABLE_ORDERING = Ordering.from(new Comparator<Relatable>() {
        public int compare(Relatable l1, Relatable l2) {
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

    public Map<String, String> getNamespaces() {
        return ImmutableMap.copyOf(namespaces);
    }

    public List<Link> getCanonicalLinks() {
        return ImmutableList.copyOf(links);
    }

    public List<Link> getLinksByRel(final String rel) {
        final String resolvedRelType = resolvableUri.matcher(rel).matches() ? resolveRelativeHref(rel) : rel;
        final String curiedRel = currieHref(resolvedRelType);
        return ImmutableList.copyOf(Iterables.filter(getLinks(), new Predicate<Relatable>() {
            public boolean apply(@Nullable Relatable relatable) {
                return Iterables.contains(Splitter.on(" ").split(relatable.getRel()), curiedRel);
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
            String rels = Joiner.on(" ").join(usingToString().sortedCopy(transform(relTypes, new Function<String, String>() {
                public String apply(@Nullable String relType) {
                    return currieHref(relType);
                }
            })));

            String curiedHref = currieHref(href);

            collatedLinks.add(new Link(curiedHref, rels));
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

    public RenderableResource asRenderableResource() {
        return new ImmutableResource(resourceFactory, getNamespaces(), getCanonicalLinks(), getProperties(), getResources());
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
