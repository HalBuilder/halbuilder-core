package com.theoryinpractise.halbuilder.impl.representations;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.theoryinpractise.halbuilder.AbstractRepresentationFactory;
import com.theoryinpractise.halbuilder.api.Contract;
import com.theoryinpractise.halbuilder.api.Link;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.Rel;
import com.theoryinpractise.halbuilder.api.RepresentationException;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.impl.api.Support;
import com.theoryinpractise.halbuilder.impl.bytecode.InterfaceContract;
import com.theoryinpractise.halbuilder.impl.bytecode.InterfaceRenderer;
import javaslang.Function1;
import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.collection.HashSet;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.collection.Set;
import javaslang.collection.TreeMap;
import javaslang.collection.TreeSet;
import javaslang.control.Option;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.google.common.base.Strings.emptyToNull;
import static com.theoryinpractise.halbuilder.api.Rels.getRel;
import static com.theoryinpractise.halbuilder.impl.api.Support.WHITESPACE_SPLITTER;

public abstract class BaseRepresentation
    implements ReadableRepresentation {

  public static final Comparator<Link> RELATABLE_ORDERING =
      Comparator.comparing(Link::getRel,
          (r1, r2) -> {
            if (r1.contains("self")) {
              return -1;
            }
            if (r2.contains("self")) {
              return 1;
            }
            return r1.compareTo(r2);
          });

  protected NamespaceManager namespaceManager = NamespaceManager.EMPTY;
  protected Option<String> content = Option.none();
  protected TreeMap<String, Rel> rels = TreeMap.empty();
  protected List<Link> links = List.empty();
  protected TreeMap<String, Option<Object>> properties = TreeMap.empty();
  protected Multimap<String, ReadableRepresentation> resources = ArrayListMultimap.create();
  protected boolean hasNullProperties = false;
  protected AbstractRepresentationFactory representationFactory;

  protected BaseRepresentation(AbstractRepresentationFactory representationFactory, final Option<String> content) {
    this.representationFactory = representationFactory;
    this.content = content;
  }

  private String toString(String contentType, final Set<URI> flags) {
    try {
      ByteArrayOutputStream boas = new ByteArrayOutputStream();
      OutputStreamWriter osw = new OutputStreamWriter(boas, StandardCharsets.UTF_8);
      toString(contentType, flags, osw);
      return boas.toString(StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      throw new RepresentationException("Unable to write representation: " + e.getMessage(), e);
    }
  }

  private void toString(String contentType, Set<URI> flags, Writer writer) {
    validateNamespaces(this);
    representationFactory.lookupRenderer(contentType)
                         .write(this, flags.addAll(representationFactory.getFlags()), writer);
  }

  protected void validateNamespaces(ReadableRepresentation representation) {
    representation.getCanonicalLinks()
                  .forEach(l -> namespaceManager.validateNamespaces(l.getRel()));

    representation.getResources().forEach(r -> {
      namespaceManager.validateNamespaces(r._1());
      validateNamespaces(r._2());
    });

  }

  @Override
  public Option<String> getContent() {
    return content;
  }

  public Option<Link> getResourceLink() {
    return getLinkByRel(Support.SELF);
  }

  public Map<String, String> getNamespaces() {
    return namespaceManager.getNamespaces();
  }

  public List<Link> getCanonicalLinks() {
    return getNaturalLinks();
  }

  public List<Link> getLinks() {
    if (representationFactory.getFlags().contains(RepresentationFactory.COALESCE_LINKS)) {
      return getCollatedLinks();
    } else {
      return getNaturalLinks();
    }
  }

  public Option<Link> getLinkByRel(String rel) {
    return getLinksByRel(rel).headOption();
  }

  public Option<Link> getLinkByRel(Rel rel) {
    return getLinkByRel(getRel(rel));
  }

  public List<Link> getLinksByRel(final String rel) {
    Support.checkRelType(rel);
    final String curiedRel = namespaceManager.currieHref(rel);
    return getLinksByRel(this, curiedRel);
  }

  public List<Link> getLinksByRel(final Rel rel) {
    return getLinksByRel(getRel(rel));
  }

  public List<? extends ReadableRepresentation> getResourcesByRel(final String rel) {
    Support.checkRelType(rel);

    return List.ofAll(resources.get(rel));
  }

  public List<? extends ReadableRepresentation> getResourcesByRel(final Rel rel) {
    return getResourcesByRel(getRel(rel));
  }

  public Option<Object> getValue(String name) {
    return properties.get(name)
                     .flatMap(val -> val);
  }

  public Object getValue(String name, Object defaultValue) {
    return getValue(name).orElse(defaultValue);
  }

  public Map<String, Option<Object>> getProperties() {
    return properties;
  }

  public boolean hasNullProperties() {
    return hasNullProperties;
  }

  public List<Tuple2<String, ReadableRepresentation>> getResources() {
    return List.ofAll(resources.entries())
               .map(e -> Tuple.of(e.getKey(), e.getValue()));
  }

  public Map<String, List<? extends ReadableRepresentation>> getResourceMap() {

    final java.lang.Iterable<Tuple2<String, List<ReadableRepresentation>>> entries
        = resources.asMap().entrySet().stream()
                   .map(e -> Tuple.of(e.getKey(), List.ofAll(e.getValue())))
                   .collect(Collectors.toList());

    return TreeMap.ofAll(entries);

  }

  /**
   * Test whether the Representation in its current state satisfies the provided interface.
   *
   * @param contract The interface we wish to check
   *
   * @return Is that Representation satisfied by the supplied contract?
   */
  public boolean isSatisfiedBy(Contract contract) {
    return contract.isSatisfiedBy(this);
  }

  /**
   * Renders the current Representation as a proxy to the provider interface.
   *
   * @param anInterface The interface we wish to proxy the resource as
   *
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
    return toString(contentType, HashSet.empty());
  }

  @Override
  public String toString(String contentType, URI... flags) {
    return toString(contentType, HashSet.of(flags));
  }

  public void toString(String contentType, Writer writer) {
    toString(contentType, HashSet.empty(), writer);
  }

  @Override
  public void toString(String contentType, Writer writer, URI... flags) {
    toString(contentType, HashSet.of(flags), writer);
  }

  private List<Link> getCollatedLinks() {
    List<Link> collatedLinks = List.empty();

    // href, rel, link
    Table<String, String, Link> linkTable = HashBasedTable.create();

    for (Link link : links) {
      linkTable.put(link.getHref(), link.getRel(), link);
    }

    for (String href : linkTable.rowKeySet()) {
      Set<String> relTypes = TreeSet.ofAll(linkTable.row(href).keySet());
      Collection<Link> hrefLinks = linkTable.row(href).values();

      String rels = mkSortableJoiner(" ", relTypes)
                        .apply(relType -> namespaceManager.currieHref(relType));

      Function1<Function1<Link, String>, String> nameFunc = mkSortableJoiner(", ", hrefLinks);

      String titles = nameFunc.apply(Link::getTitle);

      String names = nameFunc.apply(Link::getName);

      String hreflangs = nameFunc.apply(Link::getHreflang);

      String profile = nameFunc.apply(Link::getProfile);

      collatedLinks = collatedLinks.append(new Link(rels, href, emptyToNull(names),
                                                       emptyToNull(titles),
                                                       emptyToNull(hreflangs),
                                                       emptyToNull(profile)));
    }

    return collatedLinks.sort(RELATABLE_ORDERING);
  }

  private static <T> Function1<Function1<T, String>, String> mkSortableJoiner(final String join, final Iterable<T> ts) {
    return new Function1<Function1<T, String>, String>() {
      @Nullable
      @Override
      public String apply(Function1<T, String> f) {
        return StreamSupport.stream(ts.spliterator(), false)
                            .map(f::apply)
                            .filter(Objects::nonNull)
                            .sorted()
                            .distinct()
                            .collect(Collectors.joining(join));
      }
    };
  }

  private List<Link> getNaturalLinks() {
    return links.map(link -> new Link(namespaceManager.currieHref(link.getRel()),
                                         link.getHref(), link.getName(), link.getTitle(),
                                         link.getHreflang(), link.getProfile()))
                .sort(RELATABLE_ORDERING);

  }

  private List<Link> getLinksByRel(ReadableRepresentation representation, String rel) {
    Support.checkRelType(rel);
    return representation
               .getCanonicalLinks()
               .filter(link -> rel.equals(link.getRel())
                               || Iterables.contains(WHITESPACE_SPLITTER.split(link.getRel()),
                   rel));
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
    return getLinkByRel("self")
               .map(href -> "<Representation: " + href.getHref() + ">")
               .orElse("<Representation: @" + Integer.toHexString(hashCode()) + ">");
  }

}
