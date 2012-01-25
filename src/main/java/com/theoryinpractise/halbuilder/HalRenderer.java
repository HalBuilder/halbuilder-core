package com.theoryinpractise.halbuilder;

import com.google.common.base.Optional;
import com.theoryinpractise.halbuilder.HalResource;

import java.io.Writer;

public interface HalRenderer<T> {
    Optional<T> render(HalResource resource, Writer writer);
}
