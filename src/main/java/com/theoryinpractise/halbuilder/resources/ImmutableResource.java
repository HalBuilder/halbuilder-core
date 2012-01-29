package com.theoryinpractise.halbuilder.resources;

import com.google.common.collect.Multimap;
import com.theoryinpractise.halbuilder.Link;
import com.theoryinpractise.halbuilder.ReadableResource;
import com.theoryinpractise.halbuilder.Resource;
import com.theoryinpractise.halbuilder.ResourceFactory;

import java.util.List;
import java.util.Map;

public class ImmutableResource extends MutableResource {

    public ImmutableResource(ResourceFactory resourceFactory,
                             Map<String, String> namespaces, List<Link> links, Map<String, Object> properties, Multimap<String, ReadableResource> resources) {
        super(resourceFactory);
        this.namespaces = namespaces;
        this.links = links;
        this.properties = properties;
        this.resources = resources;
    }

    @Override
    public MutableResource withLink(String href, String rel) {
        throw new UnsupportedOperationException("ImmutableResources cannot be mutated.");
    }

    @Override
    public MutableResource withLink(Link link) {
        throw new UnsupportedOperationException("ImmutableResources cannot be mutated.");
    }

    @Override
    public Resource withProperty(String name, Object value) {
        throw new UnsupportedOperationException("ImmutableResources cannot be mutated.");
    }

    @Override
    public Resource withBean(Object value) {
        throw new UnsupportedOperationException("ImmutableResources cannot be mutated.");
    }

    @Override
    public Resource withFields(Object value) {
        throw new UnsupportedOperationException("ImmutableResources cannot be mutated.");
    }

    @Override
    public Resource withFieldBasedSubresource(String rel, String href, Object o) {
        throw new UnsupportedOperationException("ImmutableResources cannot be mutated.");
    }

    @Override
    public Resource withBeanBasedSubresource(String rel, String href, Object o) {
        throw new UnsupportedOperationException("ImmutableResources cannot be mutated.");
    }

    @Override
    public Resource withNamespace(String namespace, String href) {
        throw new UnsupportedOperationException("ImmutableResources cannot be mutated.");
    }

    @Override
    public MutableResource withSubresource(String rel, Resource resource) {
        throw new UnsupportedOperationException("ImmutableResources cannot be mutated.");
    }
}
