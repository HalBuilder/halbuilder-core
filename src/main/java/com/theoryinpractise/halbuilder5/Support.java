package com.theoryinpractise.halbuilder5;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;

public interface Support {

  Splitter WHITESPACE_SPLITTER = Splitter.onPattern("\\s").omitEmptyStrings();
  String REL = "rel";
  String SELF = "self";
  String LINK = "link";
  String HREF = "href";
  String LINKS = "_links";
  String EMBEDDED = "_embedded";
  String NAME = "name";
  String CURIES = "curies";
  String TITLE = "title";
  String HREFLANG = "hreflang";
  String TEMPLATED = "templated";

  String PROFILE = "profile";

  static void checkRelType(String rel) {
    Preconditions.checkArgument(rel != null, "Provided rel should not be null.");
    Preconditions.checkArgument(
        !"".equals(rel) && !rel.contains(" "),
        "Provided rel value should be a single rel type, as " + "defined by http://tools.ietf.org/html/rfc5988");
  }
}
