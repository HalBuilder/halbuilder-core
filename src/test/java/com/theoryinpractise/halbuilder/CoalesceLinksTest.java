package com.theoryinpractise.halbuilder;

import com.google.common.collect.Iterables;
import com.theoryinpractise.halbuilder.api.Link;
import com.theoryinpractise.halbuilder.api.Representation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
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
        .has(new ContainsRelCondition("bar"))
        .has(new ContainsRelCondition("foo"));

    assertThat(resource.getLinksByRel("bar"))
        .isNotNull()
        .has(new ContainsRelCondition("bar"))
        .doesNotHave(new ContainsRelCondition("foo"));

    assertThat(resource.getLinksByRel("foo"))
        .isNotNull()
        .doesNotHave(new ContainsRelCondition("bar"))
        .has(new ContainsRelCondition("foo"));

  }

  @Test
  public void testCoalesceLinks() {

    Representation resource = new DefaultRepresentationFactory()
                                  .withFlag(RepresentationFactory.COALESCE_LINKS)
                                  .newRepresentation("/foo")
                                  .withLink("bar", "/bar")
                                  .withLink("foo", "/bar");

    assertThat(resource.getLinks())
        .isNotEmpty()
        .has(new ContainsRelCondition("bar foo"))
        .has(new ContainsRelCondition("bar"))
        .has(new ContainsRelCondition("foo"));

    assertThat(resource.getLinksByRel("bar"))
        .isNotNull()
        .has(new ContainsRelCondition("bar"))
        .doesNotHave(new ContainsRelCondition("foo"));

    assertThat(resource.getLinksByRel("foo"))
        .isNotNull()
        .doesNotHave(new ContainsRelCondition("bar"))
        .has(new ContainsRelCondition("foo"));
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

    resource.getLinkByRel(null);
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

  private static class ContainsRelCondition
      extends Condition<Iterable<Link>> {

    private final String rel;

    public ContainsRelCondition(final String rel) {
      this.rel = rel;
    }

    @Override
    public boolean matches(Iterable<Link> objects) {
      boolean hasMatch = false;
      for (Link link : objects) {
        if (rel.equals(link.getRel()) || Iterables.contains(WHITESPACE_SPLITTER.split(link.getRel()), rel)) {
          hasMatch = true;
        }
      }
      return hasMatch;
    }

  }
}
