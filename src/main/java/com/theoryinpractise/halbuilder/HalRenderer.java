package com.theoryinpractise.halbuilder;

import com.theoryinpractise.halbuilder.HalResource;

import java.io.Writer;

public interface HalRenderer {
    void render(HalResource resource, Writer writer);
}
