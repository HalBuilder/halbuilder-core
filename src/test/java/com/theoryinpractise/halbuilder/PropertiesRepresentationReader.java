package com.theoryinpractise.halbuilder;

import com.google.common.base.Throwables;
import com.google.common.io.CharStreams;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.Representation;
import com.theoryinpractise.halbuilder.api.RepresentationReader;
import com.theoryinpractise.halbuilder.impl.representations.PersistentRepresentation;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Properties;

/**
 * RepresentationReader for java.util.Properties like files.
 */
public class PropertiesRepresentationReader
    implements RepresentationReader {
  private final AbstractRepresentationFactory representationFactory;

  public PropertiesRepresentationReader(AbstractRepresentationFactory representationFactory) {
    this.representationFactory = representationFactory;
  }

  public ReadableRepresentation read(Reader reader) {
    try {
      String source = CharStreams.toString(reader);
      Representation sbr = new PersistentRepresentation(representationFactory, source);
      Properties properties = new Properties();

      properties.load(new StringReader(source));

      for (String key : properties.stringPropertyNames()) {
        sbr = sbr.withProperty(key, properties.getProperty(key));
      }
      return sbr;
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }
}
