package com.theoryinpractise.halbuilder5;

import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.Tuple;
import javaslang.Tuple2;
import org.derive4j.ArgOption;
import org.derive4j.Data;
import org.derive4j.Flavour;

import java.util.regex.Pattern;

import static com.theoryinpractise.halbuilder5.Links.getHref;

/**
 * A Link to an external resource.
 */
@Data(flavour = Flavour.Javaslang, arguments = ArgOption.checkedNotNull)
public abstract class Link {

  public static final String NAME = "name";
  public static final String TITLE = "title";
  public static final String HREFLANG = "hreflang";
  public static final String PROFILE = "profile";
  public static final String REL = "rel";
  public static final String SELF = "self";
  public static final String LINK = "link";
  public static final String HREF = "href";

  /**
   * Pattern that will hit an RFC 6570 URI template.
   */
  private static final Pattern URI_TEMPLATE_PATTERN = Pattern.compile("\\{.+\\}");

  interface Cases<R> {
    R simple(String rel, String href);

    R full(String rel, String href, Map<String, String> properties);
  }

  public abstract <R> R match(Cases<R> cases);

  private boolean hasTemplate = false;

  /**
   * Determine whether the argument href contains at least one URI template, as defined in RFC 6570.
   *
   * @return True if the href contains a template, false if not (or if the argument is null).
   */
  public boolean hasTemplate() {
    CharSequence href = getHref(this);
    return (href != null) && URI_TEMPLATE_PATTERN.matcher(href).find();
  }

  private List<Tuple2<String, String>> templateFragement() {
    return hasTemplate() ? List.of(Tuple.of("templated", "true")) : List.empty();
  }

  private List<Tuple2<String, String>> generateLinkFragments() {
    List<Tuple2<String, String>> linkFragments =
        Links.cases()
            .simple((rel, href) -> List.of(Tuple.of("rel", rel), Tuple.of("href", href)))
            .full(
                (rel, href, properties) ->
                    List.of(Tuple.of("rel", rel), Tuple.of("href", href)).appendAll(properties))
            .apply(this);

    return linkFragments.appendAll(templateFragement());
  }

  @Override
  public String toString() {
    return "<link "
        + generateLinkFragments().map(it -> it._1 + "=\"" + it._2 + "\"").mkString(" ")
        + "/>";
  }

  @Override
  public abstract boolean equals(Object o);

  @Override
  public abstract int hashCode();
}
