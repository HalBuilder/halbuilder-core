package com.theoryinpractise.halbuilder;

import com.theoryinpractise.halbuilder.api.ContentRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.impl.representations.ImmutableRepresentation;
import com.theoryinpractise.halbuilder.impl.representations.MutableRepresentation;
import org.testng.annotations.Test;

import java.io.StringReader;

import static com.google.common.truth.Truth.assertThat;

public class RepresentationFactoryTest {

  private static final String TEXT_X_JAVA_PROPERTIES = "text/x-java-properties";

  private static final RepresentationFactory representationFactory =
      new DefaultRepresentationFactory().withReader(TEXT_X_JAVA_PROPERTIES, PropertiesRepresentationReader.class);

  @Test
  public void testWithCustomReader() {

    String source = "name=dummy";

    ContentRepresentation representation = representationFactory.readRepresentation(TEXT_X_JAVA_PROPERTIES, new StringReader(source));
    assertThat(representation.getProperties().get("name")).isEqualTo("dummy");
    assertThat(representation.getContent()).isEqualTo(source);
  }

  @Test
  public void testImmutableRepresentation() {

    MutableRepresentation rep = (MutableRepresentation) representationFactory.newRepresentation();
    rep.withProperty("name", "Test");
    ImmutableRepresentation immutableRep = rep.toImmutableResource();
    assertThat(immutableRep.getValue("name")).isEqualTo("Test");
  }
}
