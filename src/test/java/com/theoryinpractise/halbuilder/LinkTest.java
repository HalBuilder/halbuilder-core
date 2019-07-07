package com.theoryinpractise.halbuilder;

import static org.fest.assertions.api.Assertions.assertThat;

import com.theoryinpractise.halbuilder.api.Link;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import org.testng.annotations.Test;

public class LinkTest {
  private RepresentationFactory representationFactory = new DefaultRepresentationFactory();

  private Link link = new Link(representationFactory, "rel", "http://example.com/", "name", "title", "hreflang", "");

  @Test
  public void equalLinksHaveEqualHashCodes() {
    Link otherLink = new Link(representationFactory, "rel", "http://example.com/", "name", "title", "hreflang", "");
    assertThat(link.hashCode()).isEqualTo(otherLink.hashCode());
  }

  @Test
  public void testHashCodeIsDependentOnHref() {
    Link otherLink = new Link(representationFactory, "rel", "http://example.com/other", "name", "title", "hreflang", "");
    assertThat(otherLink.hashCode()).isNotEqualTo(link.hashCode());
  }

  @Test
  public void testHashCodeIsDependentOnRel() {
    Link otherLink = new Link(representationFactory, "otherrel", "http://example.com/", "name", "title", "hreflang", "");
    assertThat(otherLink.hashCode()).isNotEqualTo(link.hashCode());
  }

  @Test
  public void testHashCodeIsDependentOnName() {
    Link otherLink = new Link(representationFactory, "rel", "http://example.com/", "othername", "title", "hreflang", "");
    assertThat(otherLink.hashCode()).isNotEqualTo(link.hashCode());
  }

  @Test
  public void testHashCodeIsDependentOnTitle() {
    Link otherLink = new Link(representationFactory, "rel", "http://example.com/", "name", "othertitle", "hreflang", "");
    assertThat(otherLink.hashCode()).isNotEqualTo(link.hashCode());
  }

  @Test
  public void testHashCodeIsDependentOnHreflang() {
    Link otherLink = new Link(representationFactory, "rel", "http://example.com/other", "name", "title", "hreflang", "");
    assertThat(otherLink.hashCode()).isNotEqualTo(link.hashCode());
  }

  @Test
  public void testEqualsIsDependentOnHref() {
    Link otherLink = new Link(representationFactory, "rel", "http://example.com/other", "name", "title", "hreflang", "");
    assertThat(otherLink).isNotEqualTo(link);
  }

  @Test
  public void testEqualsIsDependentOnRel() {
    Link otherLink = new Link(representationFactory, "otherrel", "http://example.com/", "name", "title", "hreflang", "");
    assertThat(otherLink).isNotEqualTo(link);
  }

  @Test
  public void testEqualsIsDependentOnName() {
    Link otherLink = new Link(representationFactory, "rel", "http://example.com/", "othername", "title", "hreflang", "");
    assertThat(otherLink).isNotEqualTo(link);
  }

  @Test
  public void testEqualsIsDependentOnTitle() {
    Link otherLink = new Link(representationFactory, "rel", "http://example.com/", "name", "othertitle", "hreflang", "");
    assertThat(otherLink).isNotEqualTo(link);
  }

  @Test
  public void testEqualsIsDependentOnHreflang() {
    Link otherLink = new Link(representationFactory, "rel", "http://example.com/other", "name", "title", "hreflang", "");
    assertThat(otherLink).isNotEqualTo(link);
  }

  @Test
  public void testToStringRendersHrefRel() {
    String toString = new Link(representationFactory, "rel", "http://example.com/other").toString();
    assertThat(toString).isEqualTo("<link rel=\"rel\" href=\"http://example.com/other\"/>");
  }

  @Test
  public void testToStringRendersHrefRelNameTitleHreflang() {
    String toString = link.toString();
    assertThat(toString).isEqualTo("<link rel=\"rel\" href=\"http://example.com/\" name=\"name\" title=\"title\" " + "hreflang=\"hreflang\"/>");
  }

  @Test
  public void testHasTemplate() {
    Link templateLink = new Link(representationFactory, "customerSearch", "http://example.com/search{?customerId}");
    assertThat(templateLink.hasTemplate()).isTrue();
  }

  @Test
  public void testDoesNotHaveTemplate() {
    Link nonTemplateLink = new Link(representationFactory, "rel", "http://example.com/other");
    assertThat(nonTemplateLink.hasTemplate()).isFalse();
  }
}
