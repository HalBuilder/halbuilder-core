package com.theoryinpractise.halbuilder;

import com.google.common.base.Function;
import com.google.common.base.Optional;

import java.util.List;
import java.util.Map;

public interface ReadableResource extends Relatable {
    Link getSelfLink();

    Map<String, String> getNamespaces();

    List<Link> getCanonicalLinks();

    List<Link> getLinks();

    List<Link> getLinksByRel(String rel);

    Map<String, Object> getProperties();

    List<Resource> getResources();

    <T> boolean isSatisfiedBy(Contract contract);

    <T, V> Optional<V> ifSatisfiedBy(Class<T> anInterface, Function<T, V> function);

    RenderableResource asRenderableResource();

    ReadableResource asImmutableResource();
}
