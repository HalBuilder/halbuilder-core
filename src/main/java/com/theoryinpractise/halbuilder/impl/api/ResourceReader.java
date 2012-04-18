package com.theoryinpractise.halbuilder.impl.api;

import java.io.Reader;

import com.theoryinpractise.halbuilder.impl.resources.ImmutableResource;

public interface ResourceReader {
    ImmutableResource read(Reader source);
}
