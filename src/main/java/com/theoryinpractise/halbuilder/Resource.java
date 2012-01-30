package com.theoryinpractise.halbuilder;

public interface Resource extends ReadableResource {

    Resource withLink(String url, String rel);

    Resource withLink(Link link);

    Resource withProperty(String name, Object value);

    Resource withBean(Object value);

    Resource withFields(Object value);

    Resource withFieldBasedSubresource(String rel, String href, Object o);

    Resource withBeanBasedSubresource(String rel, String href, Object o);

    Resource withNamespace(String namespace, String url);

    Resource withSubresource(String rel, Resource resource);

}
