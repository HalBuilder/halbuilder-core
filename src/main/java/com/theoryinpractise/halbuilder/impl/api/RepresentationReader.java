package com.theoryinpractise.halbuilder.impl.api;

import com.theoryinpractise.halbuilder.spi.ReadableRepresentation;

import java.io.Reader;

public interface RepresentationReader {
    ReadableRepresentation read(Reader source);
}
