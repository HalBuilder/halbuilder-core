package com.theoryinpractise.halbuilder5;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContentType {
  private String type;
  private String subType;

  public ContentType(String contentType) {
    Pattern contentTypePattern = Pattern.compile("([\\w|\\*]*)/([^;,\\s]*)");
    Matcher matcher = contentTypePattern.matcher(contentType);
    if (matcher.find()) {
      type = matcher.group(1);
      subType = matcher.group(2);
    }
  }

  @Override
  public int hashCode() {
    int result = type.hashCode();
    return 31 * result + subType.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ContentType that = (ContentType) o;

    if (!subType.equals(that.subType)) {
      return false;
    }
    return type.equals(that.type);
  }

  public boolean matches(String contentType) {
    return matches(new ContentType(contentType));
  }

  public boolean matches(ContentType contentType) {

    if (typeMatches(getType(), contentType.getType())) {
      if (typeMatches(getSubType(), contentType.getSubType())) {
        return true;
      }
    }

    return false;
  }

  private boolean typeMatches(String left, String right) {
    return (left.equals(right) || "*".equals(right));
  }

  public String getType() {
    return type;
  }

  public String getSubType() {
    return subType;
  }
}
