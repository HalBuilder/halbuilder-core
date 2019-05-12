package com.theoryinpractise.halbuilder;

import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.api.ContentRepresentation;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.StringReader;

import static org.fest.assertions.api.Assertions.assertThat;

public class RepresentationFactoryTest {

  private static final String TEXT_X_JAVA_PROPERTIES = "text/x-java-properties";

  @Test
  public void testWithCustomReader() throws IOException {
    RepresentationFactory representationFactory =
        new DefaultRepresentationFactory()
            .withReader(TEXT_X_JAVA_PROPERTIES, PropertiesRepresentationReader.class);

    String source = "name=dummy";

    ContentRepresentation representation =
        representationFactory.readRepresentation(TEXT_X_JAVA_PROPERTIES, new StringReader(source));
    assertThat(representation.getProperties().get("name")).isEqualTo("dummy");
    assertThat(representation.getContent()).isEqualTo(source);
  }
}
