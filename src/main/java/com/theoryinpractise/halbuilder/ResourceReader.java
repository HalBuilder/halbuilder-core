package com.theoryinpractise.halbuilder;

import java.io.BufferedReader;
import java.io.Reader;

public interface ResourceReader {
    ReadableResource read(Reader source);
}
