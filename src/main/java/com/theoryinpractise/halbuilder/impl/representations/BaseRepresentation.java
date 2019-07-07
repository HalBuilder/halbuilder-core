package com.theoryinpractise.halbuilder.impl.representations;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
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
import com.theoryinpractise.halbuilder.AbstractRepresentationFactory;
import com.theoryinpractise.halbuilder.api.Contract;
import com.theoryinpractise.halbuilder.api.Link;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationException;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.api.RepresentationWriter;
import com.theoryinpractise.halbuilder.impl.api.Support;
import com.theoryinpractise.halbuilder.impl.bytecode.InterfaceContract;
import com.theoryinpractise.halbuilder.impl.bytecode.InterfaceRenderer;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Ordering.usingToString;
import static com.google.common.collect.Sets.newHashSet;
import static com.theoryinpractise.halbuilder.impl.api.Support.WHITESPACE_SPLITTER;

public abstract class BaseRepresentation implements ReadableRepresentation {

  public static final Ordering<Link> RELATABLE_ORDERING =
      Ordering.from(
          (l1, l2) -> {
            if (l1.getRel().contains("self")) {
              return -1;
            }
            if (l2.getRel().contains("self")) {
              return 1;
            }
            return l1.getRel().compareTo(l2.getRel());
          });

  protected NamespaceManager namespaceManager = new NamespaceManager();

  protected List<Link> links = Lists.newArrayList();
  protected Map<String, Object> properties = Maps.newTreeMap(usingToString());
  protected Multimap<String, ReadableRepresentation> resources = ArrayListMultimap.create();

  protected AbstractRepresentationFactory representationFactory;
  protected boolean hasNullProperties = false;

  protected BaseRepresentation(AbstractRepresentationFactory representationFactory) {
    this.representationFactory = representationFactory;
  }

  @Override
  public Link getResourceLink() {
    return Iterables.find(getLinks(), LinkPredicate.newLinkPredicate(Support.SELF), null);
  }

  @Override
  public Map<String, String> getNamespaces() {
    return ImmutableMap.copyOf(namespaceManager.getNamespaces());
  }

  @Override
  public List<Link> getCanonicalLinks() {
    return ImmutableList.copyOf(getNaturalLinks());
  }

  @Override
  public Link getLinkByRel(String rel) {
    return Iterables.getFirst(getLinksByRel(rel), null);
  }

  @Override
  public List<Link> getLinksByRel(final String rel) {
    Support.checkRelType(rel);

    final String curiedRel = namespaceManager.currieHref(rel);
    final ImmutableList.Builder<Link> linkBuilder = ImmutableList.builder();

    linkBuilder.addAll(getLinksByRel(this, curiedRel));

    return linkBuilder.build();
  }

  @Override
  public List<? extends ReadableRepresentation> getResourcesByRel(final String rel) {
    Support.checkRelType(rel);

    return ImmutableList.copyOf(resources.get(rel));
  }

  @Override
  public Object getValue(String name) {
    if (properties.containsKey(name)) {
      return properties.get(name);
    } else {
      throw new RepresentationException("Resource does not contain " + name);
    }
  }

  @Override
  public Object getValue(String name, Object defaultValue) {
    try {
      return getValue(name);
    } catch (RepresentationException e) {
      return defaultValue;
    }
  }

  private List<Link> getLinksByRel(ReadableRepresentation representation, final String rel) {
    Support.checkRelType(rel);
    return ImmutableList.copyOf(
        Iterables.filter(
            representation.getCanonicalLinks(),
            relatable -> rel.equals(relatable.getRel()) || Iterables.contains(WHITESPACE_SPLITTER.split(relatable.getRel()), rel)));
  }

  @Override
  public List<Link> getLinks() {
    if (representationFactory.getFlags().contains(RepresentationFactory.COALESCE_LINKS)) {
      return getCollatedLinks();
    } else {
      return getNaturalLinks();
    }
  }

  private List<Link> getNaturalLinks() {
    return FluentIterable.from(links)
        .transform(
            link ->
                new Link(
                    representationFactory,
                    namespaceManager.currieHref(link.getRel()),
                    link.getHref(),
                    link.getName(),
                    link.getTitle(),
                    link.getHreflang(),
                    link.getProfile()))
        .toSortedList(RELATABLE_ORDERING);
  }

  private List<Link> getCollatedLinks() {
    List<Link> collatedLinks = Lists.newArrayList();

    // href, rel, link
    Table<String, String, Link> linkTable = HashBasedTable.create();

    for (Link link : links) {
      linkTable.put(link.getHref(), link.getRel(), link);
    }

    for (String href : linkTable.rowKeySet()) {
      Set<String> relTypes = linkTable.row(href).keySet();
      Collection<Link> hrefLinks = linkTable.row(href).values();

      String rels = mkSortableJoinerForIterable(" ", relTypes).apply(relType -> namespaceManager.currieHref(relType));

      Function<Function<Link, String>, String> nameFunc = mkSortableJoinerForIterable(", ", hrefLinks);

      String titles = nameFunc.apply(link -> link.getTitle());

      String names = nameFunc.apply(link -> link.getName());

      String hreflangs = nameFunc.apply(link -> link.getHreflang());

      String profile = nameFunc.apply(link -> link.getProfile());

      collatedLinks.add(new Link(representationFactory, rels, href, emptyToNull(names), emptyToNull(titles), emptyToNull(hreflangs), emptyToNull(profile)));
    }

    return RELATABLE_ORDERING.sortedCopy(collatedLinks);
  }

  private <T> Function<Function<T, String>, String> mkSortableJoinerForIterable(final String join, final Iterable<T> ts) {
    return new Function<Function<T, String>, String>() {
      @Nullable
      @Override
      public String apply(Function<T, String> f) {
        return Joiner.on(join).skipNulls().join(usingToString().nullsFirst().sortedCopy(newHashSet(transform(ts, f))));
      }
    };
  }

  @Override
  public Map<String, Object> getProperties() {
    return Collections.unmodifiableMap(properties);
  }

  @Override
  public Collection<Map.Entry<String, ReadableRepresentation>> getResources() {
    return ImmutableMultimap.copyOf(resources).entries();
  }

  @Override
  public Map<String, Collection<ReadableRepresentation>> getResourceMap() {
    return ImmutableMap.copyOf(resources.asMap());
  }

  protected void validateNamespaces(ReadableRepresentation representation) {
    for (Link link : representation.getCanonicalLinks()) {
      namespaceManager.validateNamespaces(link.getRel());
    }
    for (Map.Entry<String, ReadableRepresentation> aResource : representation.getResources()) {
      namespaceManager.validateNamespaces(aResource.getKey());
      validateNamespaces(aResource.getValue());
    }
  }

  /**
   * Test whether the Representation in its current state satisfies the provided interface.
   *
   * @param contract The interface we wish to check
   * @return Is that Representation satisfied by the supplied contract?
   */
  @Override
  public boolean isSatisfiedBy(Contract contract) {
    return contract.isSatisfiedBy(this);
  }

  @Override
  public boolean hasNullProperties() {
    return hasNullProperties;
  }

  public ImmutableRepresentation toImmutableResource() {
    return new ImmutableRepresentation(representationFactory, namespaceManager, getCanonicalLinks(), getProperties(), getResources(), hasNullProperties);
  }

  /**
   * Renders the current Representation as a proxy to the provider interface
   *
   * @param anInterface The interface we wish to proxy the resource as
   * @return A Guava Optional of the rendered class, this will be absent if the interface doesn't satisfy the interface
   */
  @Override
  public <T> T toClass(Class<T> anInterface) {
    if (InterfaceContract.newInterfaceContract(anInterface).isSatisfiedBy(this)) {
      return InterfaceRenderer.newInterfaceRenderer(anInterface).render(this);
    } else {
      throw new RepresentationException("Unable to write representation to " + anInterface.getName());
    }
  }

  @Override
  public String toString(String contentType) {
    return toString(contentType, Collections.emptySet());
  }

  /** @deprecated */
  @Override
  @Deprecated
  public String toString(String contentType, final Set<URI> flags) {
    ByteArrayOutputStream boas = new ByteArrayOutputStream();
    OutputStreamWriter osw = new OutputStreamWriter(boas, StandardCharsets.UTF_8);
    toString(contentType, flags, osw);
    return boas.toString();
  }

  @Override
  public void toString(String contentType, Writer writer) {
    toString(contentType, Collections.emptySet(), writer);
  }

  /** @deprecated */
  @Override
  @Deprecated
  public void toString(String contentType, Set<URI> flags, Writer writer) {
    validateNamespaces(this);
    RepresentationWriter<String> representationWriter = representationFactory.lookupRenderer(contentType);
    ImmutableSet.Builder<URI> uriBuilder = ImmutableSet.<URI>builder().addAll(representationFactory.getFlags());
    if (flags != null) {
      uriBuilder.addAll(flags);
    }
    representationWriter.write(this, uriBuilder.build(), writer);
  }

  @Override
  public String toString(String contentType, URI... flags) {
    return toString(contentType, ImmutableSet.copyOf(flags));
  }

  @Override
  public void toString(String contentType, Writer writer, URI... flags) {
    toString(contentType, ImmutableSet.copyOf(flags), writer);
  }

  @Override
  public int hashCode() {
    int h = namespaceManager.hashCode();
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
    boolean e = this.namespaceManager.equals(that.namespaceManager);
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
      return "<Representation: @" + Integer.toHexString(hashCode()) + ">";
    }
  }
}
