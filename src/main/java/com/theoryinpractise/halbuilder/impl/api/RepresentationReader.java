package com.theoryinpractise.halbuilder.impl.api;

import com.theoryinpractise.halbuilder.api.ReadableRepresentation;

import java.io.Reader;

public interface RepresentationReader {
    ReadableRepresentation read(Reader source);
}
