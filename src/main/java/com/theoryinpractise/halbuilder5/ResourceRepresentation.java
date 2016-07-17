package com.theoryinpractise.halbuilder5;

import com.google.common.collect.Iterables;
import javaslang.Value;
import javaslang.collection.Iterator;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.collection.Multimap;
import javaslang.collection.Traversable;
import javaslang.collection.TreeMap;
import javaslang.collection.TreeMultimap;
import javaslang.control.Option;
import okio.ByteString;

import java.net.URI;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.theoryinpractise.halbuilder5.Link.SELF;
import static com.theoryinpractise.halbuilder5.Rels.getRel;
import static com.theoryinpractise.halbuilder5.Support.WHITESPACE_SPLITTER;

public final class ResourceRepresentation<V> implements Value<V> {

  public static final URI PRETTY_PRINT = URI.create("urn:halbuilder:prettyprint");

  public static final URI STRIP_NULLS = URI.create("urn:halbuilder:stripnulls");

  public static final URI SILENT_SORTING = URI.create("urn:halbuilder:silentsorting");

  public static final URI HYPERTEXT_CACHE_PATTERN = URI.create("urn:halbuild:hypertextcachepattern");

  public static final Comparator<Link> RELATABLE_ORDERING =
      Comparator.comparing(
          Links::getRel,
          (r1, r2) -> {
            if (r1.contains("self")) {
              return -1;
            }
            if (r2.contains("self")) {
              return 1;
            }
            return r1.compareTo(r2);
          });

  // Implement Javaslang Value<T> methods

  @Override
  public V get() {
    return value;
  }

  @Override
  public boolean isEmpty() {
    return value == null;
  }

  @Override
  public boolean isSingleValued() {
    return true;
  }

  @Override
  public <U> ResourceRepresentation<U> map(Function<? super V, ? extends U> function) {
    return new ResourceRepresentation<U>(content, links, rels, namespaceManager, function.apply(value), resources);
  }

  @Override
  public ResourceRepresentation<V> peek(Consumer<? super V> consumer) {
    consumer.accept(value);
    return this;
  }

  @Override
  public String stringPrefix() {
    return "Representation";
  }

  @Override
  public Iterator<V> iterator() {
    return Option.of(value).iterator();
  }

  public <R> R transform(Function<V, R> transformer) {
    return transformer.apply(value);
  }

  protected NamespaceManager namespaceManager = NamespaceManager.EMPTY;

  protected Option<ByteString> content = Option.none();

  protected TreeMap<String, Rel> rels = TreeMap.empty();

  protected List<Link> links = List.empty();

  protected V value;

  protected Multimap<String, ResourceRepresentation<?>> resources = TreeMultimap.withSet().empty();

  protected boolean hasNullProperties = false;

  private ResourceRepresentation(
      Option<ByteString> content,
      List<Link> links,
      TreeMap<String, Rel> rels,
      NamespaceManager namespaceManager,
      V value,
      Multimap<String, ResourceRepresentation<?>> resources) {
    this.content = content;
    this.links = links;
    this.rels = rels;
    this.namespaceManager = namespaceManager;
    this.value = value;
    this.resources = resources;
  }

  public static ResourceRepresentation<Void> empty(String href) {
    return empty().withLink("self", href);
  }

  public static <V> ResourceRepresentation<V> create(V value) {
    return empty().withValue(value);
  }

  public static <V> ResourceRepresentation<V> create(String href, V value) {
    return empty().withLink("self", href).withValue(value);
  }

  private static final ResourceRepresentation<Void> EMPTY =
      new ResourceRepresentation<>(
          Option.none(),
          List.<Link>empty(),
          TreeMap.of("self", Rels.singleton("self")),
          NamespaceManager.EMPTY,
          null,
          TreeMultimap.withSet().empty());

  public static ResourceRepresentation<Void> empty() {
    return EMPTY;
  }

  /**
   * Retrieve the defined rel semantics for this representation.
   *
   * @return
   */
  public Map<String, Rel> getRels() {
    return rels;
  }

  /**
   * Adds or replaces the content of the representation.
   *
   * @param content The source content of the representation.
   *
   * @return A new instance of a PersistentRepresentation with the namespace included.
   */
  public ResourceRepresentation<V> withContent(ByteString content) {
    return new ResourceRepresentation<>(Option.of(content), links, rels, namespaceManager, value, resources);
  }

  /**
   * Define rel semantics for this representation.
   *
   * @param rel A defined relationship type
   */
  public ResourceRepresentation<V> withRel(Rel rel) {
    if (rels.containsKey(rel.rel())) {
      throw new IllegalStateException(String.format("Rel %s is already declared.", rel.rel()));
    }
    final TreeMap<String, Rel> updatedRels = rels.put(rel.rel(), rel);
    return new ResourceRepresentation<>(content, links, updatedRels, namespaceManager, value, resources);
  }

  /**
   * Add a link to this resource.
   *
   * @param rel
   * @param href The target href for the link, relative to the href of this resource.
   *
   * @return
   */
  public ResourceRepresentation<V> withLink(String rel, String href) {
    return withLink(Links.simple(rel, href));
  }

  /**
   * Add a link to this resource.
   *
   * @param rel
   * @param uri The target URI for the link, possibly relative to the href of this resource.
   *
   * @return
   */
  public ResourceRepresentation<V> withLink(String rel, URI uri) {
    return withLink(rel, uri.toASCIIString());
  }

  /**
   * Add a link to this resource.
   *
   * @param rel
   * @param href The target href for the link, relative to the href of this resource.
   * @param properties The properties to add to this link object
   */
  public ResourceRepresentation<V> withLink(String rel, String href, Map<String, String> properties) {
    return withLink(Links.full(rel, href, properties));
  }

  /**
   * Add a link to this resource.
   *
   * @param link The target link
   */
  public ResourceRepresentation<V> withLink(Link link) {
    String rel = Links.getRel(link);
    Support.checkRelType(rel);
    validateSingletonRel(rel);
    final TreeMap<String, Rel> updatedRels = !rels.containsKey(rel) ? rels.put(rel, Rels.natural(rel)) : rels;
    final List<Link> updatedLinks = links.append(link);
    return new ResourceRepresentation<>(content, updatedLinks, updatedRels, namespaceManager, value, resources);
  }

  /**
   * Add a link to this resource.
   *
   * @param links The target link
   */
  public ResourceRepresentation<V> withLinks(List<Link> links) {
    links.forEach(
        link -> {
          String rel = Links.getRel(link);
          Support.checkRelType(rel);
          validateSingletonRel(rel);
        });

    final TreeMap<String, Rel> updatedRels =
        links
            .map(Links::getRel)
            .foldLeft(rels, (accum, rel) -> !accum.containsKey(rel) ? rels.put(rel, Rels.natural(rel)) : rels);

    final List<Link> updatedLinks = links.appendAll(links);
    return new ResourceRepresentation<>(content, updatedLinks, updatedRels, namespaceManager, value, resources);
  }

  /**
   * Replace the value of this resource with a new value, optionally of a new type.
   *
   * @param newValue The new value for this resource
   * @param <R> The type of the new value
   * @return The new resource
   */
  public <R> ResourceRepresentation<R> withValue(R newValue) {
    return new ResourceRepresentation<>(Option.none(), links, rels, namespaceManager, newValue, resources);
  }

  /**
   * Adds a new namespace.
   *
   * @param namespace The CURIE prefix for the namespace being added.
   * @param href The target href of the namespace being added. This may be relative to the resourceFactories baseref
   *
   * @return A new instance of a PersistentRepresentation with the namespace included.
   */
  public ResourceRepresentation<V> withNamespace(String namespace, String href) {
    if (!rels.containsKey("curies")) {
      rels = rels.put("curies", Rels.collection("curies"));
    }

    final NamespaceManager updatedNamespaceManager = namespaceManager.withNamespace(namespace, href);
    return new ResourceRepresentation<>(content, links, rels, updatedNamespaceManager, value, resources);
  }

  public ResourceRepresentation<V> withRepresentation(String rel, ResourceRepresentation<?> resource) {

    if (resources.containsValue(resource)) {
      throw new IllegalStateException("Resource is already embedded.");
    }

    Support.checkRelType(rel);
    validateSingletonRel(rel);

    Multimap<String, ResourceRepresentation<?>> updatedResources = resources.put(rel, resource);

    ResourceRepresentation<V> updatedRepresentation =
        new ResourceRepresentation<>(content, links, rels, namespaceManager, value, updatedResources);
    // Propagate null property flag to parent.
    if (resource.hasNullProperties()) {
      updatedRepresentation.hasNullProperties = true;
    }

    if (!rels.containsKey(rel)) {
      updatedRepresentation = updatedRepresentation.withRel(Rels.natural(rel));
    }

    return updatedRepresentation;
  }

  private void validateSingletonRel(String unvalidatedRel) {
    rels.get(unvalidatedRel)
        .forEach(
            rel -> {
              // Rel is register, check for duplicate singleton
              if (isSingleton(rel) && (!getLinksByRel(rel).isEmpty() || !getResourcesByRel(rel).isEmpty())) {
                throw new IllegalStateException(String.format("%s is registered as a single rel and already exists.", rel));
              }
            });
  }

  private static Boolean isSingleton(Rel rel) {
    return rel.match(
        Rels.cases((__) -> Boolean.TRUE, (__) -> Boolean.FALSE, (__) -> Boolean.FALSE, (__, id, comparator) -> Boolean.FALSE));
  }

  public void validateNamespaces() {
    getLinks().forEach(link -> namespaceManager.validateNamespaces(Links.getRel(link)));

    resources.forEach(
        (key, rel) -> {
          namespaceManager.validateNamespaces(key);
          rel.validateNamespaces();
        });
  }

  public Option<ByteString> getContent() {
    return content;
  }

  public Option<Link> getResourceLink() {
    return getLinkByRel(SELF);
  }

  public Map<String, String> getNamespaces() {
    return namespaceManager.getNamespaces();
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

  public Traversable<ResourceRepresentation<?>> getResourcesByRel(final String rel) {
    Support.checkRelType(rel);
    return resources.get(rel).getOrElse(List.empty());
  }

  public Traversable<ResourceRepresentation<?>> getResourcesByRel(final Rel rel) {
    return getResourcesByRel(getRel(rel));
  }

  public boolean hasNullProperties() {
    return hasNullProperties;
  }

  public Multimap<String, ResourceRepresentation<?>> getResources() {
    return resources;
  }

  public List<Link> getLinks() {
    return links.map(link -> Links.modRel(rel -> namespaceManager.currieHref(rel)).apply(link)).sorted(RELATABLE_ORDERING);
  }

  private List<Link> getLinksByRel(ResourceRepresentation<V> representation, String rel) {
    Support.checkRelType(rel);
    return representation
        .getLinks()
        .filter(
            link -> {
              final String linkRel = Links.getRel(link);
              return rel.equals(linkRel) || Iterables.contains(WHITESPACE_SPLITTER.split(linkRel), rel);
            });
  }

  @Override
  public int hashCode() {
    int h = namespaceManager.hashCode();
    h += links.hashCode();
    if (value != null) {
      h += value.hashCode();
    }
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
    if (!(obj instanceof ResourceRepresentation)) {
      return false;
    }
    ResourceRepresentation that = (ResourceRepresentation) obj;
    boolean e = Objects.equals(this.namespaceManager, that.namespaceManager);
    e &= Objects.equals(this.links, that.links);

    e &= Objects.equals(this.value, that.value);
    e &= Objects.equals(this.resources, that.resources);
    return e;
  }

  @Override
  public String toString() {
    return getLinkByRel("self")
        .map(link -> String.format("<Representation: %s>", Links.getHref(link)))
        .getOrElse(String.format("<Representation: @%s>", Integer.toHexString(hashCode())));
  }
}
