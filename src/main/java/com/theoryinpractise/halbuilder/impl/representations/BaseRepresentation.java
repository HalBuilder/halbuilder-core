package com.theoryinpractise.halbuilder.impl.representations;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.*;
import com.theoryinpractise.halbuilder.AbstractRepresentationFactory;
import com.theoryinpractise.halbuilder.api.*;
import com.theoryinpractise.halbuilder.impl.api.Support;
import com.theoryinpractise.halbuilder.impl.bytecode.InterfaceContract;
import com.theoryinpractise.halbuilder.impl.bytecode.InterfaceRenderer;
import fj.Ord;
import fj.data.List;
import fj.data.Option;
import fj.data.Set;
import fj.data.TreeMap;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URI;
import java.util.Collection;
import java.util.Map;

import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Ordering.usingToString;
import static com.google.common.collect.Sets.newHashSet;
import static com.theoryinpractise.halbuilder.impl.api.Support.WHITESPACE_SPLITTER;
import static fj.data.List.list;

public abstract class BaseRepresentation
    implements ReadableRepresentation {

  protected NamespaceManager                         namespaceManager  = new NamespaceManager();
  protected TreeMap<String, Rel>                     rels              = TreeMap.empty(Ord.stringOrd.reverse());
  protected List<Link>                               links             = List.nil();
  protected TreeMap<String, Option<Object>>          properties        = TreeMap.empty(Ord.stringOrd.reverse());
  protected Multimap<String, ReadableRepresentation> resources         = ArrayListMultimap.create();
  protected boolean                                  hasNullProperties = false;
  protected AbstractRepresentationFactory representationFactory;

  public static final Ord<Link> RELATABLE_ORDERING = Ord.ord(l1 -> l2 -> {
    if (l1.getRel().contains("self")) return fj.Ordering.LT;
    if (l2.getRel().contains("self")) return fj.Ordering.GT;
    return fj.Ordering.fromInt(l1.getRel().compareTo(l2.getRel()));
  });

  protected BaseRepresentation(AbstractRepresentationFactory representationFactory) {
    this.representationFactory = representationFactory;
  }

  public Link getResourceLink() {
    return Iterables.find(getLinks(), LinkPredicate.newLinkPredicate(Support.SELF), null);
  }

  public TreeMap<String, String> getNamespaces() {
    return namespaceManager.getNamespaces();
  }

  public List<Link> getCanonicalLinks() {
    return getNaturalLinks();
  }

  public List<Link> getLinks() {
    if (representationFactory.getFlags().member(RepresentationFactory.COALESCE_LINKS)) {
      return getCollatedLinks();
    } else {
      return getNaturalLinks();
    }
  }

  public Link getLinkByRel(String rel) {
    return Iterables.getFirst(getLinksByRel(rel), null);
  }

  public List<Link> getLinksByRel(final String rel) {
    Support.checkRelType(rel);
    final String curiedRel = namespaceManager.currieHref(rel);
    return getLinksByRel(this, curiedRel);
  }

  public List<? extends ReadableRepresentation> getResourcesByRel(final String rel) {
    Support.checkRelType(rel);

    return list(resources.get(rel));
  }

  public Option<Object> getValue(String name) {
    return properties.get(name)
                     .bind(val -> val);
  }

  public Object getValue(String name, Object defaultValue) {
    return getValue(name).orSome(defaultValue);
  }

  public TreeMap<String, Option<Object>> getProperties() {
    return properties;
  }

  private List<Link> getLinksByRel(ReadableRepresentation representation, String rel) {
    Support.checkRelType(rel);
    return representation.getCanonicalLinks()
                         .filter(link -> rel.equals(link.getRel())
                                         || Iterables.contains(WHITESPACE_SPLITTER.split(link.getRel()), rel));
  }

  public boolean hasNullProperties() {
    return hasNullProperties;
  }

  public Collection<Map.Entry<String, ReadableRepresentation>> getResources() {
    return ImmutableMultimap.copyOf(resources).entries();
  }

  public TreeMap<String, Collection<? extends ReadableRepresentation>> getResourceMap() {
    return TreeMap.fromMutableMap(Ord.stringOrd, ImmutableMap.copyOf(resources.asMap()));
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
      throw new RepresentationException("Unable to write representation to " + anInterface.getName());
    }
  }

  public String toString(String contentType) {
    return toString(contentType, Set.empty(Ord.hashOrd()));
  }

  @Deprecated
  public String toString(String contentType, final Set<URI> flags) {
    try {
      ByteArrayOutputStream boas = new ByteArrayOutputStream();
      OutputStreamWriter osw = new OutputStreamWriter(boas, "UTF-8");
      toString(contentType, flags, osw);
      return boas.toString();
    } catch (UnsupportedEncodingException e) {
      throw new RepresentationException("Unable to write representation: " + e.getMessage());
    }
  }

  @Override
  public String toString(String contentType, URI... flags) {
    return toString(contentType, Set.set(Ord.hashOrd(), flags));
  }

  public void toString(String contentType, Writer writer) {
    toString(contentType, Set.empty(Ord.hashOrd()), writer);
  }

  @Deprecated
  public void toString(String contentType, Set<URI> flags, Writer writer) {
    validateNamespaces(this);
    representationFactory.lookupRenderer(contentType)
                         .write(this, flags.union(representationFactory.getFlags()), writer);
  }

  @Override
  public void toString(String contentType, Writer writer, URI... flags) {
    toString(contentType, Set.set(Ord.hashOrd(), flags), writer);
  }

  private List<Link> getCollatedLinks() {
    List<Link> collatedLinks = List.nil();

    // href, rel, link
    Table<String, String, Link> linkTable = HashBasedTable.create();

    for (Link link : links) {
      linkTable.put(link.getHref(), link.getRel(), link);
    }

    for (String href : linkTable.rowKeySet()) {
      Set<String> relTypes = Set.iterableSet(Ord.<String>comparableOrd(), linkTable.row(href).keySet());
      Collection<Link> hrefLinks = linkTable.row(href).values();

      String rels = mkSortableJoinerForIterable(" ", relTypes)
                        .apply(relType -> namespaceManager.currieHref(relType));

      Function<Function<Link, String>, String> nameFunc = mkSortableJoinerForIterable(", ", hrefLinks);

      String titles = nameFunc.apply(Link::getTitle);

      String names = nameFunc.apply(Link::getName);

      String hreflangs = nameFunc.apply(Link::getHreflang);

      String profile = nameFunc.apply(Link::getProfile);

      collatedLinks = collatedLinks.cons(new Link(representationFactory, rels, href,
                                                  emptyToNull(names),
                                                  emptyToNull(titles),
                                                  emptyToNull(hreflangs),
                                                  emptyToNull(profile)
      ));
    }

    return collatedLinks.sort(RELATABLE_ORDERING);
  }

  private List<Link> getNaturalLinks() {
    return links.map(link -> new Link(representationFactory, namespaceManager.currieHref(link.getRel()),
                                      link.getHref(), link.getName(), link.getTitle(), link.getHreflang(), link.getProfile()))
                .sort(RELATABLE_ORDERING);

  }

  private <T> Function<Function<T, String>, String> mkSortableJoinerForIterable(final String join, final Iterable<T> ts) {
    return new Function<Function<T, String>, String>() {
      @Nullable
      @Override
      public String apply(Function<T, String> f) {
        return Joiner.on(join)
                     .skipNulls()
                     .join(usingToString().nullsFirst()
                                          .sortedCopy(newHashSet(transform(ts, f))));
      }
    };
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

  public ImmutableRepresentation toImmutableResource() {
    return new ImmutableRepresentation(representationFactory, namespaceManager, getCanonicalLinks(), getProperties(),
                                       getResources(), hasNullProperties);
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
