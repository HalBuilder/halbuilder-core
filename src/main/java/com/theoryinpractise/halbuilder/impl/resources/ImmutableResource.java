package com.theoryinpractise.halbuilder.impl.resources;

import com.google.common.base.Optional;
import com.theoryinpractise.halbuilder.ResourceFactory;
import com.theoryinpractise.halbuilder.impl.bytecode.InterfaceContract;
import com.theoryinpractise.halbuilder.impl.bytecode.InterfaceRenderer;
import com.theoryinpractise.halbuilder.spi.Link;
import com.theoryinpractise.halbuilder.spi.Renderer;
import com.theoryinpractise.halbuilder.spi.Resource;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;

public class ImmutableResource extends BaseResource {

    private final Link resourceLink;

    public ImmutableResource(ResourceFactory resourceFactory,
                             Map<String, String> namespaces, List<Link> links, Map<String, Optional<Object>> properties, List<Resource> resources, boolean hasNullProperties) {
        super(resourceFactory);
        this.namespaces = namespaces;
        this.links = links;
        this.properties = properties;
        this.resources = resources;

        this.resourceLink = super.getResourceLink();

        this.hasNullProperties = hasNullProperties;
    }

    public Link getResourceLink() {
        return resourceLink;
    }


}
