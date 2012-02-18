package com.theoryinpractise.halbuilder.spi;

import com.google.common.base.Optional;

import java.io.Writer;

/**
 * A Renderer takes a ReadableResource and renders it to the provided Writer, returning an
 * Optional value.
 * @param <T> A class to return.
 */
public interface Renderer<T> {

    /**
     * Returns an Optional value after writing the resource to the provided Writer.
     * @param resource The resource to render
     * @param writer The Writer to write to
     * @return An optional value
     */
    Optional<T> render(ReadableResource resource, Writer writer);
}
