package com.theoryinpractise.halbuilder.spi;

import com.google.common.base.Optional;

/**
 * A RenderableResource is a Resource that has been optimized for rendering.
 */
public interface RenderableResource extends ReadableResource {

    /**
     * Returns an optional proxy to the given interface mirroring the resource.
     * @param anInterface An interface to mirror
     * @return A Guava Optional Resource Proxy
     */
    <T> Optional<T> renderClass(Class<T> anInterface);

    /**
     * Returns the resource in the request content-type.
     *
     * application/hal+xml and application/hal+json are provided by default,
     * additional Renderers can be added to a ResourceFactory.
     *
     * @param contentType The content type requested
     * @return A String
     */
    String renderContent(String contentType);

}
