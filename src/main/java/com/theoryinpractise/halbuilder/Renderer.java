package com.theoryinpractise.halbuilder;

import com.google.common.base.Optional;

import java.io.Writer;

public interface Renderer<T> {
    Optional<T> render(ReadableResource resource, Writer writer);
}
