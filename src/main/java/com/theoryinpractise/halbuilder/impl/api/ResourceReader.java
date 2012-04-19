package com.theoryinpractise.halbuilder.impl.api;

import com.theoryinpractise.halbuilder.spi.ReadableResource;

import java.io.Reader;

public interface ResourceReader {
    ReadableResource read(Reader source);
}
