package com.theoryinpractise.halbuilder.spi;

import com.google.common.base.Optional;

import java.io.Writer;

/**
 * A Renderer takes a ReadableRepresentation and renders it to the provided Writer, returning an
 * Optional value.
 * @param <T> A class to return.
 */
public interface Renderer<T> {

    /**
     * Returns an Optional value after writing the representation to the provided Writer.
     * @param representation The representation to render
     * @param writer The Writer to write to
     * @return An optional value
     */
    Optional<T> render(ReadableRepresentation representation, Writer writer);
}
