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

    <T> Optional<T> renderClass(Class<T> anInterface);

    String renderJson();

    String renderXml();

    <T> boolean isSatisfiedBy(Contract contract);

    <T, V> Optional<V> ifSatisfiedBy(Class<T> anInterface, Function<T, V> function);
}
