package com.theoryinpractise.halbuilder;

public interface Resource extends ReadableResource {

    Resource withBaseHref(String s);

    Resource withLink(String rel, String url);

    Resource withProperty(String name, Object value);

    Resource withBean(Object value);

    Resource withFields(Object value);

    Resource withFieldBasedSubresource(String rel, String href, Object o);

    Resource withBeanBasedSubresource(String rel, String href, Object o);

    Resource withNamespace(String namespace, String url);

    Resource withSubresource(String rel, ReadableResource resource);

    ReadableResource asImmutableResource();
}
