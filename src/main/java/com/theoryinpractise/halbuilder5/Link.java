package com.theoryinpractise.halbuilder5;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import org.derive4j.ArgOption;
import org.derive4j.Data;
import org.derive4j.Derive;
import org.derive4j.ExportAsPublic;
import org.derive4j.Flavour;
import org.derive4j.Visibility;

import java.util.regex.Pattern;

/** A Link to an external resource. */
@Data(
  flavour = Flavour.Vavr,
  arguments = ArgOption.checkedNotNull,
  value = @Derive(withVisibility = Visibility.Smart)
)
public abstract class Link {

  public static final String HREF = "href";
  public static final String HREFLANG = "hreflang";
  public static final String LINK = "link";
  public static final String METHOD = "method";
  public static final String NAME = "name";
  public static final String PROFILE = "profile";
  public static final String REL = "rel";
  public static final String SELF = "self";
  public static final String TEMPLATED = "templated";
  public static final String TITLE = "title";

  /** Pattern that will hit an RFC 6570 URI template. */
  private static final Pattern URI_TEMPLATE_PATTERN = Pattern.compile("\\{.+\\}");

  interface Cases<R> {
    R simple(String rel, String href, Boolean templated);

    R full(String rel, String href, Boolean templated, Map<String, String> properties);
  }

  public abstract <R> R match(Cases<R> cases);

  private List<Tuple2<String, String>> templateFragement() {
    return Links.getTemplated(this) ? List.of(Tuple.of(TEMPLATED, "true")) : List.empty();
  }

  private List<Tuple2<String, String>> generateLinkFragments() {
    List<Tuple2<String, String>> linkFragments =
        Links.cases()
            .simple((rel, href, templated) -> List.of(Tuple.of(REL, rel), Tuple.of(HREF, href)))
            .full(
                (rel, href, templated, properties) ->
                    List.of(Tuple.of(REL, rel), Tuple.of(HREF, href)).appendAll(properties))
            .apply(this);

    return linkFragments.appendAll(templateFragement());
  }

  @Override
  public String toString() {
    return "<link "
        + generateLinkFragments().map(it -> String.format("%s=\"%s\"", it._1, it._2)).mkString(" ")
        + "/>";
  }

  @Override
  public abstract boolean equals(Object o);

  @Override
  public abstract int hashCode();

  private static boolean isTemplated(String href) {
    return (href != null) && URI_TEMPLATE_PATTERN.matcher(href).find();
  }

  @ExportAsPublic
  static Link create(String rel, String href, String... properties) {
    if (properties.length % 2 != 0) {
      throw new IllegalArgumentException("Parameter count must be even");
    }

    Map<String, String> propertyMap = HashMap.empty();
    for (int i = 0; i < properties.length; i = i + 2) {
      String key = properties[i];
      String value = properties[i + 1];
      propertyMap = propertyMap.put(key, value);
    }
    return create(rel, href, propertyMap);
  }

  @ExportAsPublic
  static Link create(String rel, String href, Map<String, String> properties) {
    return Links.full0(rel, href, isTemplated(href), properties);
  }

  @ExportAsPublic
  static Link create(String rel, String href, java.util.Map<String, String> properties) {
    return Links.full0(rel, href, isTemplated(href), HashMap.ofAll(properties));
  }

}
