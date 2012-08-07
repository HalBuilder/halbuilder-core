package com.theoryinpractise.halbuilder.spi;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;

import java.net.URI;

public interface Representation extends ReadableRepresentation {

    Representation withLink(String rel, String href);

    Representation withLink(String rel, URI uri);

    Representation withLink(String rel, String href, Predicate<ReadableRepresentation> predicate);

    Representation withLink(String rel, URI uri, Predicate<ReadableRepresentation> predicate);

    Representation withLink(String rel, String href, Optional<Predicate<ReadableRepresentation>> predicate, Optional<String> name, Optional<String> title, Optional<String> hreflang);

    Representation withLink(String rel, URI uri, Optional<Predicate<ReadableRepresentation>> predicate, Optional<String> name, Optional<String> title, Optional<String> hreflang);

    Representation withProperty(String name, Object value);

    Representation withBean(Object value);

    Representation withFields(Object value);

    Representation withSerializable(Serializable serializable);

    Representation withFieldBasedRepresentation(String rel, String href, Object o);

    Representation withBeanBasedRepresentation(String rel, String href, Object o);

    Representation withNamespace(String namespace, String href);

    Representation withRepresentation(String rel, ReadableRepresentation resource);

}
