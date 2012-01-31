package com.theoryinpractise.halbuilder.impl.api;

import com.google.common.base.Optional;
import com.theoryinpractise.halbuilder.spi.ReadableResource;

import java.io.Writer;

public interface Renderer<T> {

    Optional<T> render(ReadableResource resource, Writer writer);
}
