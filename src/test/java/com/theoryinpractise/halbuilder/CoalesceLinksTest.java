package com.theoryinpractise.halbuilder;

import com.theoryinpractise.halbuilder.api.Link;
import com.theoryinpractise.halbuilder.api.Representation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import javaslang.collection.List;
import org.testng.annotations.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.theoryinpractise.halbuilder.LinkListSubject.assertAboutLinkLists;
import static org.testng.Assert.fail;

public class CoalesceLinksTest {

  @Test
  public void testNonCoalesceLinks() {

    Representation resource = new DefaultRepresentationFactory().newRepresentation("/foo")
                                                                .withLink("bar", "/bar")
                                                                .withLink("foo", "/bar");

    assertThat(resource.getLinks()).isNotEmpty();

    assertAboutLinkLists(resource.getLinks()).containsRelCondition("bar");
    assertAboutLinkLists(resource.getLinks()).containsRelCondition("foo");

    assertThat(resource.getLinksByRel("bar")).isNotNull();
    assertAboutLinkLists(resource.getLinksByRel("bar")).containsRelCondition("bar");
    assertAboutLinkLists(resource.getLinksByRel("bar")).doesNotContainRelCondition("foo");

    assertThat(resource.getLinksByRel("foo")).isNotNull();
    assertAboutLinkLists(resource.getLinksByRel("foo")).containsRelCondition("foo");
    assertAboutLinkLists(resource.getLinksByRel("foo")).doesNotContainRelCondition("bar");

  }

  @Test
  public void testCoalesceLinks() {

    Representation resource = new DefaultRepresentationFactory()
                                  .withFlag(RepresentationFactory.COALESCE_LINKS)
                                  .newRepresentation("/foo")
                                  .withLink("bar", "/bar")
                                  .withLink("foo", "/bar");

    final List<Link> links = resource.getLinks();

    assertThat(links).isNotEmpty();
    assertAboutLinkLists(links).containsRelCondition("bar foo");
    assertAboutLinkLists(links).containsRelCondition("bar");
    assertAboutLinkLists(links).containsRelCondition("foo");

    assertThat(resource.getLinksByRel("bar")).isNotNull();
    assertAboutLinkLists(resource.getLinksByRel("bar")).containsRelCondition("bar");
    assertAboutLinkLists(resource.getLinksByRel("bar")).doesNotContainRelCondition("foo");

    assertThat(resource.getLinksByRel("foo")).isNotNull();
    assertAboutLinkLists(resource.getLinksByRel("foo")).doesNotContainRelCondition("barf");
    assertAboutLinkLists(resource.getLinksByRel("foo")).containsRelCondition("foo");
  }

  @Test
  public void testSpacedRelsSeparateLinks() {

    Representation representation = new DefaultRepresentationFactory().newRepresentation("/foo");

    try {
      representation.withLink("bar foo", "/bar");
      fail("We should fail to add a space separated link rel.");
    } catch (IllegalArgumentException e) {
      // expected
    }

  }

  @Test
  public void testMultiSpacedRelsSeparateLinks() {

    Representation representation = new DefaultRepresentationFactory().newRepresentation("/foo");
    try {
      representation.withLink("bar                  foo", "/bar");
      fail("We should fail to add a space separated link rel.");
    } catch (IllegalArgumentException e) {
      // expected
    }

  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testRelLookupsWithNullFail() {
    Representation resource = new DefaultRepresentationFactory().newRepresentation("/foo")
                                                                .withLink("bar foo", "/bar");

    resource.getLinkByRel((String) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testRelLookupsWithEmptyRelFail() {
    Representation resource = new DefaultRepresentationFactory().newRepresentation("/foo")
                                                                .withLink("bar", "/bar");

    resource.getLinkByRel("");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testRelLookupsWithSpacesFail() {
    Representation resource = new DefaultRepresentationFactory().newRepresentation("/foo")
                                                                .withLink("bar", "/bar");

    resource.getLinkByRel("test fail");
  }

}
