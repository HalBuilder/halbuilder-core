package com.theoryinpractise.halbuilder;

import com.google.common.collect.Iterables;
import com.theoryinpractise.halbuilder.api.Link;
import com.theoryinpractise.halbuilder.api.Representation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import javaslang.collection.List;
import org.fest.assertions.core.Condition;
import org.testng.annotations.Test;

import static com.theoryinpractise.halbuilder.impl.api.Support.WHITESPACE_SPLITTER;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;

public class CoalesceLinksTest {

  @Test
  public void testNonCoalesceLinks() {

    Representation resource = new DefaultRepresentationFactory().newRepresentation("/foo")
                                                                .withLink("bar", "/bar")
                                                                .withLink("foo", "/bar");

    assertThat(resource.getLinks())
        .isNotEmpty()
        .has(containsRelCondition("bar"))
        .has(containsRelCondition("foo"));

    assertThat(resource.getLinksByRel("bar"))
        .isNotNull()
        .has(containsRelCondition("bar"))
        .doesNotHave(containsRelCondition("foo"));

    assertThat(resource.getLinksByRel("foo"))
        .isNotNull()
        .doesNotHave(containsRelCondition("bar"))
        .has(containsRelCondition("foo"));

  }

  private static Condition<Iterable<Link>> containsRelCondition(String rel) {
    return new Condition<Iterable<Link>>() {
      @Override
      public boolean matches(final Iterable<Link> links) {
        boolean hasMatch = false;
        for (Link link : links) {
          if (rel.equals(link.getRel()) || Iterables.contains(WHITESPACE_SPLITTER.split(link.getRel()), rel)) {
            hasMatch = true;
          }
        }
        return hasMatch;
      }
    };
  }

  @Test
  public void testCoalesceLinks() {

    Representation resource = new DefaultRepresentationFactory()
                                  .withFlag(RepresentationFactory.COALESCE_LINKS)
                                  .newRepresentation("/foo")
                                  .withLink("bar", "/bar")
                                  .withLink("foo", "/bar");

    final List<Link> links = resource.getLinks();

    assertThat(links)
        .isNotEmpty()
        .has(containsRelCondition("bar foo"))
        .has(containsRelCondition("bar"))
        .has(containsRelCondition("foo"));

    assertThat(resource.getLinksByRel("bar"))
        .isNotNull()
        .has(containsRelCondition("bar"))
        .doesNotHave(containsRelCondition("foo"));

    assertThat(resource.getLinksByRel("foo"))
        .isNotNull()
        .doesNotHave(containsRelCondition("bar"))
        .has(containsRelCondition("foo"));
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
