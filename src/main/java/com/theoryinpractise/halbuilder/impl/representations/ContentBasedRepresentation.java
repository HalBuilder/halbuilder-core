package com.theoryinpractise.halbuilder.impl.representations;

import com.theoryinpractise.halbuilder.AbstractRepresentationFactory;
import com.theoryinpractise.halbuilder.api.ContentRepresentation;

public class ContentBasedRepresentation extends MutableRepresentation implements ContentRepresentation {

  private String source;

  public ContentBasedRepresentation(AbstractRepresentationFactory representationFactory, String source, String href) {
    super(representationFactory, href);
    this.source = source;
  }

  public ContentBasedRepresentation(AbstractRepresentationFactory representationFactory, String source) {
    super(representationFactory);
    this.source = source;
  }

  @Override
  public String getContent() {
    return source;
  }

}
