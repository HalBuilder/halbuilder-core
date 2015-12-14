package com.theoryinpractise.halbuilder;

import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import javaslang.control.Option;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.StringReader;

import static com.google.common.truth.Truth.assertThat;

public class RepresentationFactoryTest {

  private static final String TEXT_X_JAVA_PROPERTIES = "text/x-java-properties";

  @Test
  public void testWithCustomReader()
      throws IOException {
    RepresentationFactory representationFactory = new DefaultRepresentationFactory()
                                                      .withReader(TEXT_X_JAVA_PROPERTIES, PropertiesRepresentationReader.class);

    String source = "name=dummy";

    ReadableRepresentation representation = representationFactory.readRepresentation(
        TEXT_X_JAVA_PROPERTIES, new StringReader(source));

    assertThat(representation.getProperties().get("name")).isEqualTo(Option.of(Option.of("dummy")));
    assertThat(representation.getValue("name")).isEqualTo(Option.of("dummy"));

  }

}
