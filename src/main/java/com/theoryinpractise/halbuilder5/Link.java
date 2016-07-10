package com.theoryinpractise.halbuilder5;

import javaslang.Function1;
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

  /**
   * Pattern that will hit an RFC 6570 URI template.
   */
  private static final Pattern URI_TEMPLATE_PATTERN = Pattern.compile("\\{.+\\}");

  interface Cases<R> {
    // A request can either be a 'GET' (of a path):
    R simple(String rel, String href);

    R named(String rel, String href, String name);

    R full(String rel, String href, String name, String title, String hreflang, String profile);
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

  private static final Function1<Link, String> toStringFn =
      Links.cases()
          .simple((rel, href) -> String.format("<link rel=\"%s\" href=\"%s\"/>", rel, href))
          .named((rel, href, name) -> String.format("<link rel=\"%s\" href=\"%s\" name=\"%s\"/>", rel, href, name))
          .full(
              (rel, href, name, title, hreflang, profile) ->
                  String.format(
                      "<link rel=\"%s\" href=\"%s\" name=\"%s\" title=\"%s\" hreflang=\"%s\" profile=\"%s\"/>",
                      rel,
                      href,
                      name,
                      title,
                      hreflang,
                      profile));

  @Override
  public String toString() {
    return toStringFn.apply(this);
  }

  @Override
  public abstract boolean equals(Object o);

  @Override
  public abstract int hashCode();
}
