package com.theoryinpractise.halbuilder.impl.api;

import java.io.Reader;

import com.theoryinpractise.halbuilder.spi.ReadableResource;

public interface ResourceReader {
    ReadableResource read(Reader source);
}
