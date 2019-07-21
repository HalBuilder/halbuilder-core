package com.theoryinpractise.halbuilder;

import com.theoryinpractise.halbuilder.api.Representation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import org.testng.annotations.Test;

import static com.theoryinpractise.halbuilder.RepresentationSubject.assertThatRepresentation;
import static org.testng.Assert.fail;

public class CoalesceLinksTest {

  @Test
  public void testNonCoalesceLinks() {

    Representation resource = new DefaultRepresentationFactory().newRepresentation("/foo").withLink("bar", "/bar").withLink("foo", "/bar");

    assertThatRepresentation(resource).containsRel("bar");
    assertThatRepresentation(resource).containsRel("foo");

    assertThatRepresentation(resource).containsRel("bar", "bar");
    assertThatRepresentation(resource).doesNotContainRel("bar", "foo");

    assertThatRepresentation(resource).containsRel("foo", "foo");
    assertThatRepresentation(resource).doesNotContainRel("foo", "bar");
  }

  @Test
  public void testCoalesceLinks() {

    Representation resource =
        new DefaultRepresentationFactory()
            .withFlag(RepresentationFactory.COALESCE_LINKS)
            .newRepresentation("/foo")
            .withLink("bar", "/bar")
            .withLink("foo", "/bar");

    assertThatRepresentation(resource).containsRel("bar foo");
    assertThatRepresentation(resource).containsRel("bar");
    assertThatRepresentation(resource).containsRel("foo");

    assertThatRepresentation(resource).containsRel("bar", "bar");
    assertThatRepresentation(resource).doesNotContainRel("bar", "foo");

    assertThatRepresentation(resource).containsRel("foo", "foo");
    assertThatRepresentation(resource).doesNotContainRel("foo", "bar");
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
  @SuppressWarnings("NullAway")
  public void testRelLookupsWithNullFail() {
    Representation resource = new DefaultRepresentationFactory().newRepresentation("/foo").withLink("bar foo", "/bar");

    resource.getLinkByRel(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testRelLookupsWithEmptyRelFail() {
    Representation resource = new DefaultRepresentationFactory().newRepresentation("/foo").withLink("bar", "/bar");

    resource.getLinkByRel("");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testRelLookupsWithSpacesFail() {
    Representation resource = new DefaultRepresentationFactory().newRepresentation("/foo").withLink("bar", "/bar");

    resource.getLinkByRel("test fail");
  }
}
