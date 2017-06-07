package com.theoryinpractise.halbuilder5;

import org.derive4j.ArgOption;
import org.derive4j.Data;
import org.derive4j.Derive;
import org.derive4j.Flavour;
import org.derive4j.Make;

import java.util.Comparator;
import java.util.function.Function;

/** Rel defines the base class of a Algebraic Data Type for relationship semantics. */
@Data(
  flavour = Flavour.Vavr,
  arguments = ArgOption.checkedNotNull,
  value = @Derive(make = {Make.constructors, Make.getters, Make.casesMatching})
)
public abstract class Rel {

  public static final Comparator<ResourceRepresentation<?>> naturalComparator =
      Comparator.comparing(rep -> rep.getResourceLink().map(Links::getRel).getOrElse(""));

  @Override
  public abstract String toString();

  @Override
  public abstract boolean equals(Object o);

  @Override
  public abstract int hashCode();

  private static final Function<Rel, String> fullRellF =
      Rels.cases()
          .singleton(rel -> rel)
          .natural(rel -> rel)
          .collection(rel -> rel)
          .sorted((rel, id, comarator) -> String.format("%s sorted:%s", rel, id));

  public String fullRel() {
    return fullRellF.apply(this);
  }

  public abstract <R> R match(Cases<R> cases);

  public String rel() {
    return Rels.getRel(this);
  }

  /**
   * The data type covers three separate cases: singleton, natural, and sorted.
   *
   * @param <R> The return type used in the various derive4j generated mapping functions.
   */
  interface Cases<R> {

    /**
     * `singleton` relationships are checked for uniqueness, and render directly as an object (
     * rather than array of objects ) when rendered as JSON.
     *
     * @param rel The relationship type
     */
    R singleton(String rel);

    /**
     * `natural` relationships are rendered in natural order, and are rendered as a list of objects,
     * or a coalesced into a single object.
     *
     * @param rel The relationship type
     */
    R natural(String rel);

    /**
     * `collection` relationships are rendered in natural order, and are ALWAYS rendered as a list
     * of objects.
     *
     * @param rel The relationship type
     */
    R collection(String rel);

    /**
     * `sorted` relationships are rendered in the order mandated by the associated `Comparator` and
     * are rendered as a list of objects.
     *
     * @param rel The relationship type
     * @param id An identifier to associate with the sorting technique used.
     * @param comparator The comparator to use when sorting representations.
     */
    R sorted(String rel, String id, Comparator<ResourceRepresentation<?>> comparator);
  }
}
