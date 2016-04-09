package com.theoryinpractise.halbuilder5;

import org.derive4j.ArgOption;
import org.derive4j.Data;
import org.derive4j.Flavour;
import org.derive4j.Visibility;
import static org.derive4j.Make.catamorphism;
import static org.derive4j.Make.constructors;
import static org.derive4j.Make.getters;
import static org.derive4j.Make.lazyConstructor;
import static org.derive4j.Make.modifiers;
import static org.derive4j.Make.patternMatching;
import static org.derive4j.Make.lambdaVisitor;
import static org.derive4j.Visibility.Same;

import java.util.Comparator;

/**
 * Rel defines the base class of a Algebraic Data Type for relationship semantics.
 */
@Data(flavour = Flavour.Javaslang, arguments = ArgOption.checkedNotNull)
public abstract class Rel {

  public static final Comparator<ResourceRepresentation<?>> naturalComparator =
      Comparator.comparing(rep -> rep.getResourceLink().map(Links::getRel).getOrElse(""));

  @Override
  public abstract String toString();

  @Override
  public abstract boolean equals(Object o);

  @Override
  public abstract int hashCode();

  public String fullRel() {
    return this.match(
        Rels.cases((rel) -> rel, (rel) -> rel, (rel) -> rel, (rel, id, comarator) -> String.format("%s sorted:%s", rel, id)));
  }

  public abstract <R> R match(Cases<R> cases);

  public String rel() {
    return this.match(Rels.cases((rel) -> rel, (rel) -> rel, (rel) -> rel, (rel, id, comarator) -> rel));
  }

  /**
   * The data type covers three separate cases: singleton, natural, and sorted.
   *
   * @param <R> The return type used in the various derive4j generated mapping functions.
   */
  interface Cases<R> {

    /**
     * `singleton` relationships are checked for uniqueness, and render directly as an object ( rather than array of objects )
     * when rendered as JSON.
     *
     * @param rel The relationship type
     */
    R singleton(String rel);

    /**
     * `natural` relationships are rendered in natural order, and are rendered as a list of objects, or a coalesced into a single
     * object.
     *
     * @param rel The relationship type
     */
    R natural(String rel);

    /**
     * `collection` relationships are rendered in natural order, and are ALWAYS rendered as a list of objects.
     *
     * @param rel The relationship type
     */
    R collection(String rel);

    /**
     * `sorted` relationships are rendered in the order mandated by the associated `Comparator` and are rendered as a list of
     * objects.
     *
     * @param rel The relationship type
     * @param id An identifier to associate with the sorting technique used.
     * @param comparator The comparator to use when sorting representations.
     */
    R sorted(String rel, String id, Comparator<ResourceRepresentation<?>> comparator);
  }
}
