package com.theoryinpractise.halbuilder;

import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.api.RepresentationWriter;

public abstract class AbstractRepresentationFactory extends RepresentationFactory {

  public abstract RepresentationWriter<String> lookupRenderer(String contentType);

}
