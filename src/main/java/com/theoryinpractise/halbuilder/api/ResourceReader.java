package com.theoryinpractise.halbuilder.api;

import java.io.Reader;

public interface ResourceReader {
    ReadableResource read(Reader source);
}
