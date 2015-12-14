package com.theoryinpractise.halbuilder;

import com.theoryinpractise.halbuilder.api.Rels;
import org.testng.annotations.Test;

import static org.testng.Assert.fail;

public class SingleLinksTest {

  @Test
  public void testDuplicateSingleLinksFails() {

    try {
      new DefaultRepresentationFactory()
          .withRel(Rels.singleton("bar"))
          .newRepresentation("/foo")
          .withLink("bar", "/bar")
          .withLink("bar", "/bar");

      fail("This should have failed with an InvalidStateException.)");

    } catch (IllegalStateException exected) {
      //
    }
  }

  @Test
  public void testDuplicateSingleEmbedFails() {

    try {
      final DefaultRepresentationFactory factory = new DefaultRepresentationFactory();

      factory
          .withRel(Rels.singleton("bar"))
          .newRepresentation("/foo")
          .withRepresentation("bar", factory.newRepresentation().withProperty("id", 1))
          .withRepresentation("bar", factory.newRepresentation().withProperty("id", 1));

      fail("This should have failed with an InvalidStateException.)");

    } catch (IllegalStateException exected) {
      //
    }
  }

}
