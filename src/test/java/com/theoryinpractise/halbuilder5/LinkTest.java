package com.theoryinpractise.halbuilder5;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class LinkTest {

  private Link link = Links.full("rel", "http://example.com/", "name", "title", "hreflang", "");

  @Test
  public void equalLinksHaveEqualHashCodes() {
    Link otherLink = Links.full("rel", "http://example.com/", "name", "title", "hreflang", "");
    assertThat(link.hashCode()).isEqualTo(otherLink.hashCode());
  }

  @Test
  public void testHashCodeIsDependentOnHref() {
    Link otherLink = Links.full("rel", "http://example.com/other", "name", "title", "hreflang", "");
    assertThat(otherLink.hashCode()).isNotEqualTo(link.hashCode());
  }

  @Test
  public void testHashCodeIsDependentOnRel() {
    Link otherLink = Links.full("otherrel", "http://example.com/", "name", "title", "hreflang", "");
    assertThat(otherLink.hashCode()).isNotEqualTo(link.hashCode());
  }

  @Test
  public void testHashCodeIsDependentOnName() {
    Link otherLink = Links.full("rel", "http://example.com/", "othername", "title", "hreflang", "");
    assertThat(otherLink.hashCode()).isNotEqualTo(link.hashCode());
  }

  @Test
  public void testHashCodeIsDependentOnTitle() {
    Link otherLink = Links.full("rel", "http://example.com/", "name", "othertitle", "hreflang", "");
    assertThat(otherLink.hashCode()).isNotEqualTo(link.hashCode());
  }

  @Test
  public void testHashCodeIsDependentOnHreflang() {
    Link otherLink = Links.full("rel", "http://example.com/other", "name", "title", "hreflang", "");
    assertThat(otherLink.hashCode()).isNotEqualTo(link.hashCode());
  }

  @Test
  public void testEqualsIsDependentOnHref() {
    Link otherLink = Links.full("rel", "http://example.com/other", "name", "title", "hreflang", "");
    assertThat(otherLink).isNotEqualTo(link);
  }

  @Test
  public void testEqualsIsDependentOnRel() {
    Link otherLink = Links.full("otherrel", "http://example.com/", "name", "title", "hreflang", "");
    assertThat(otherLink).isNotEqualTo(link);
  }

  @Test
  public void testEqualsIsDependentOnName() {
    Link otherLink = Links.full("rel", "http://example.com/", "othername", "title", "hreflang", "");
    assertThat(otherLink).isNotEqualTo(link);
  }

  @Test
  public void testEqualsIsDependentOnTitle() {
    Link otherLink = Links.full("rel", "http://example.com/", "name", "othertitle", "hreflang", "");
    assertThat(otherLink).isNotEqualTo(link);
  }

  @Test
  public void testEqualsIsDependentOnHreflang() {
    Link otherLink = Links.full("rel", "http://example.com/other", "name", "title", "hreflang", "");
    assertThat(otherLink).isNotEqualTo(link);
  }

  @Test
  public void testToStringRendersHrefRel() {
    String toString = Links.simple("rel", "http://example.com/other").toString();
    assertThat(toString).isEqualTo("<link rel=\"rel\" href=\"http://example.com/other\"/>");
  }

  @Test
  public void testToStringRendersHrefRelNameTitleHreflang() {
    String toString = link.toString();
    assertThat(toString)
        .isEqualTo(
            "<link rel=\"rel\" href=\"http://example.com/\" name=\"name\" title=\"title\" hreflang=\"hreflang\" profile=\"\"/>");
  }

  @Test
  public void testHasTemplate() {
    Link templateLink = Links.simple("customerSearch", "http://example.com/search{?customerId}");
    assertThat(templateLink.hasTemplate()).isTrue();
  }

  @Test
  public void testDoesNotHaveTemplate() {
    Link nonTemplateLink = Links.simple("rel", "http://example.com/other");
    assertThat(nonTemplateLink.hasTemplate()).isFalse();
  }
}
