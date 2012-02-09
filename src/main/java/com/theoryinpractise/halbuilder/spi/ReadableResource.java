package com.theoryinpractise.halbuilder.spi;

import com.google.common.base.Function;
import com.google.common.base.Optional;

import java.util.List;
import java.util.Map;

public interface ReadableResource {
    Link getResourceLink();

    Map<String, String> getNamespaces();

    List<Link> getCanonicalLinks();

    List<Link> getLinks();

    List<Link> getLinksByRel(String rel);

    Map<String, Object> getProperties();

    List<Resource> getResources();

    <T> boolean isSatisfiedBy(Contract contract);

    <T, V> Optional<V> ifSatisfiedBy(Class<T> anInterface, Function<T, V> function);

    RenderableResource asRenderableResource();

}
