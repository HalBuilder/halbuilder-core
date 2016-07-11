package com.theoryinpractise.halbuilder5;

import org.testng.annotations.Test;
import javaslang.collection.HashMap;

import static com.google.common.truth.Truth.assertThat;

public class LinkTest {

  private Link link = Links.full("rel", "http://example.com/", HashMap.of("name", "name").put("title", "title"));

  @Test
  public void equalLinksHaveEqualHashCodes() {
    Link otherLink = Links.full("rel", "http://example.com/", HashMap.of("name", "name").put("title", "title"));
    assertThat(link.hashCode()).isEqualTo(otherLink.hashCode());
  }

  @Test
  public void testHashCodeIsDependentOnHref() {
    Link otherLink = Links.full("rel", "http://example.com/other", HashMap.of("name", "name").put("title", "title"));

    assertThat(otherLink.hashCode()).isNotEqualTo(link.hashCode());
  }

  @Test
  public void testHashCodeIsDependentOnRel() {
    Link otherLink = Links.full("otherrel", "http://example.com/", HashMap.of("name", "name").put("title", "title"));

    assertThat(otherLink.hashCode()).isNotEqualTo(link.hashCode());
  }

  @Test
  public void testHashCodeIsDependentOnProperty() {
    Link otherLink = Links.full("rel", "http://example.com/", HashMap.of("name", "othername").put("title", "title"));

    assertThat(otherLink.hashCode()).isNotEqualTo(link.hashCode());
  }

  @Test
  public void testEqualsIsDependentOnHref() {
    Link otherLink = Links.full("rel", "http://example.com/other", HashMap.of("name", "name").put("title", "title"));

    assertThat(otherLink).isNotEqualTo(link);
  }

  @Test
  public void testEqualsIsDependentOnRel() {
    Link otherLink = Links.full("otherrel", "http://example.com/", HashMap.of("name", "name").put("title", "title"));

    assertThat(otherLink).isNotEqualTo(link);
  }

  @Test
  public void testEqualsIsDependentOnProperty() {
    Link otherLink = Links.full("rel", "http://example.com/", HashMap.of("name", "othername").put("title", "title"));

    assertThat(otherLink).isNotEqualTo(link);
  }

  @Test
  public void testToStringRenders() {
    String toString = link.toString();
    assertThat(toString).isEqualTo("<link rel=\"rel\" href=\"http://example.com/\" name=\"name\" title=\"title\"/>");
  }

  @Test
  public void testHasTemplate() {
    Link templateLink = Links.simple("customerSearch", "http://example.com/search{?customerId}");
    assertThat(templateLink.hasTemplate()).isTrue();
    System.out.println(templateLink.toString());
    assertThat(templateLink.toString()).contains("templated");
  }

  @Test
  public void testDoesNotHaveTemplate() {
    Link nonTemplateLink = Links.simple("rel", "http://example.com/other");
    assertThat(nonTemplateLink.hasTemplate()).isFalse();
  }
}
