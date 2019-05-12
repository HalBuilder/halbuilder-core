package com.theoryinpractise.halbuilder;

import com.google.common.base.Throwables;
import com.google.common.io.CharStreams;
import com.theoryinpractise.halbuilder.api.ContentRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationReader;
import com.theoryinpractise.halbuilder.impl.representations.ContentBasedRepresentation;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Properties;

/** RepresentationReader for java.util.Properties like files. */
public class PropertiesRepresentationReader implements RepresentationReader {
  private final AbstractRepresentationFactory representationFactory;

  public PropertiesRepresentationReader(AbstractRepresentationFactory representationFactory) {
    this.representationFactory = representationFactory;
  }

  @Override
  public ContentRepresentation read(Reader reader) {
    try {
      String source = CharStreams.toString(reader);
      ContentBasedRepresentation sbr =
          new ContentBasedRepresentation(representationFactory, source);
      Properties properties = new Properties();
      properties.load(new StringReader(source));
      for (String key : properties.stringPropertyNames()) {
        sbr.withProperty(key, properties.getProperty(key));
      }
      return sbr;
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }
}
