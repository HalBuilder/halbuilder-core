package com.theoryinpractise.halbuilder.spi;

import com.google.common.base.Optional;

public interface RenderableResource extends ReadableResource {

    <T> Optional<T> renderClass(Class<T> anInterface);

    String renderJson();

    String renderXml();

}
