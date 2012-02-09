package com.theoryinpractise.halbuilder.impl.api;

import com.theoryinpractise.halbuilder.spi.ReadableResource;
import com.theoryinpractise.halbuilder.spi.RenderableResource;

import java.io.Reader;

public interface ResourceReader {
    RenderableResource read(Reader source);
}
