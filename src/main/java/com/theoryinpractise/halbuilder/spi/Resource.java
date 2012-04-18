package com.theoryinpractise.halbuilder.spi;

import java.net.URI;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;

public interface Resource extends ReadableResource {

    Resource withLink(String href, String rel);

    Resource withLink(URI uri, String rel);

    Resource withLink(String href, String rel, Predicate<ReadableResource> predicate);

    Resource withLink(URI uri, String rel, Predicate<ReadableResource> predicate);

    Resource withLink(String href, String rel, Optional<Predicate<ReadableResource>> predicate, Optional<String> name, Optional<String> title, Optional<String> hreflang);

    Resource withLink(URI uri, String rel, Optional<Predicate<ReadableResource>> predicate, Optional<String> name, Optional<String> title, Optional<String> hreflang);

    Resource withProperty(String name, Object value);

    Resource withBean(Object value);

    Resource withFields(Object value);

    Resource withSerializable(Serializable serializable);

    Resource withFieldBasedSubresource(String rel, String href, Object o);

    Resource withBeanBasedSubresource(String rel, String href, Object o);

    Resource withNamespace(String namespace, String url);

    Resource withSubresource(String rel, Resource resource);

}
