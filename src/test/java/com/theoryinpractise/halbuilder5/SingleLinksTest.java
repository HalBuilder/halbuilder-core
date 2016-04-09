package com.theoryinpractise.halbuilder5;

import org.junit.Test;

public class SingleLinksTest {

  @Test
  public void testDuplicateSingleLinksFails() {

    try {
      ResourceRepresentation.empty("/foo").withRel(Rels.singleton("bar")).withLink("bar", "/bar").withLink("bar", "/bar");

      throw new AssertionError("This should have failed with an IllegalStateException.)");

    } catch (IllegalStateException exected) {
      //
    }
  }

  @Test
  public void testDuplicateSingleEmbedFails() {

    try {
      ResourceRepresentation.empty("/foo")
          .withRel(Rels.singleton("bar"))
          .withRepresentation("bar", ResourceRepresentation.empty())
          .withRepresentation("bar", ResourceRepresentation.empty());

      throw new AssertionError("This should have failed with an IllegalStateException.)");

    } catch (IllegalStateException exected) {
      //
    }
  }
}
