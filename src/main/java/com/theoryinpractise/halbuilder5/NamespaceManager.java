package com.theoryinpractise.halbuilder5;

import io.vavr.Tuple2;
import io.vavr.collection.Map;
import io.vavr.collection.TreeMap;
import io.vavr.control.Either;

import static java.lang.String.format;

/**
 * The NamespaceManager contains a mapping between CURIE prefixes and their associated HREF's, this
 * class is now backed by a persistent TreeMap and also operates as a persistent data structure.
 * Adding new namespaces DOES NOT mutate the existing instance.
 */
public class NamespaceManager {

  public static final NamespaceManager EMPTY = new NamespaceManager(TreeMap.empty());
  private final Map<String, String> namespaces;

  private NamespaceManager(final Map<String, String> namespaces) {
    this.namespaces = namespaces;
  }

  public Map<String, String> getNamespaces() {
    return namespaces;
  }

  /**
   * Update the list of declared namespaces with a new namespace.
   *
   * @param namespace Namespace curie identifier
   * @param href Namesapce URL
   * @return A new instance of the namespace manager with the additional namespace.
   */
  public NamespaceManager withNamespace(String namespace, String href) {
    if (namespaces.containsKey(namespace)) {
      throw new RepresentationException(
          format("Duplicate namespace '%s' found for representation factory", namespace));
    }
    if (!href.contains("{rel}")) {
      throw new RepresentationException(
          format("Namespace '%s' does not include {rel} URI template argument.", namespace));
    }
    return new NamespaceManager(namespaces.put(namespace, href));
  }

  public void validateNamespaces(String rel) {
    if (!rel.contains("://") && rel.contains(":")) {
      String[] relPart = rel.split(":");
      if (!namespaces.containsKey(relPart[0])) {
        throw new RepresentationException(
            format("Undeclared namespace in rel %s for resource", rel));
      }
    }
  }

  public String currieHref(String href) {
    for (Tuple2<String, String> entry : namespaces.toStream()) {

      String nsRef = entry._2;
      int startIndex = nsRef.indexOf("{rel}");
      int endIndex = startIndex + 5;

      String left = nsRef.substring(0, startIndex);
      String right = nsRef.substring(endIndex);

      if (href.startsWith(left) && href.endsWith(right)) {
        return entry._1 + ":" + href.substring(startIndex, endIndex - 2);
      }
    }
    return href;
  }

  public Either<RepresentationException, String> resolve(String ns) {
    if (!ns.contains(":")) {
      return Either.left(
          new RepresentationException("Namespaced value does not include : - not namespaced?"));
    }

    String[] parts = ns.split(":");
    String prefix = parts[0];
    String suffix = parts[1];

    return Either.right(
        namespaces
            .get(prefix)
            .map(curry -> curry.replace("{rel}", suffix))
            .getOrElse(
                () -> {
                  throw new RepresentationException("Unknown namespace key: " + prefix);
                }));
  }

  @Override
  public int hashCode() {
    return namespaces != null ? namespaces.hashCode() : 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    NamespaceManager that = (NamespaceManager) o;

    if (namespaces != null ? !namespaces.equals(that.namespaces) : that.namespaces != null) {
      return false;
    }

    return true;
  }
}
