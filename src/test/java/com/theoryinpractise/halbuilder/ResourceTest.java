package com.theoryinpractise.halbuilder;

import com.theoryinpractise.halbuilder.api.RepresentationException;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import org.testng.annotations.Test;

public class ResourceTest {

  private RepresentationFactory representationFactory = new DefaultRepresentationFactory();

  @Test(expectedExceptions = RepresentationException.class)
  public void testUndeclaredLinkNamespace() {
    representationFactory
        .newRepresentation("/test")
        .withLink("td:test", "http://localhost/test/2")
        .toString(RepresentationFactory.HAL_XML);
  }

  @Test(expectedExceptions = RepresentationException.class)
  public void testUndeclaredResourceNamespace() {
    representationFactory
        .newRepresentation("http://localhost/test")
        .withRepresentation("td:test", representationFactory.newRepresentation("/"))
        .toString(RepresentationFactory.HAL_XML);
  }

  @Test(expectedExceptions = RepresentationException.class)
  public void testUndeclaredResourceLinkNamespace() {
    representationFactory
        .newRepresentation("http://localhost/test")
        .withRepresentation(
            "test", representationFactory.newRepresentation("/").withLink("td:test", "/"))
        .toString(RepresentationFactory.HAL_XML);
  }

  @Test(expectedExceptions = RepresentationException.class)
  public void testDuplicatePropertyDefinitions() {
    representationFactory
        .newRepresentation("http://localhost/test")
        .withProperty("name", "Example User")
        .withProperty("name", "Example User")
        .toString(RepresentationFactory.HAL_XML);
  }
}
