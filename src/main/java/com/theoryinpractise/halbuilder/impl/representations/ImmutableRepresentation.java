package com.theoryinpractise.halbuilder.impl.representations;

import com.google.common.base.Optional;
import com.google.common.collect.Multimap;
import com.theoryinpractise.halbuilder.RepresentationFactory;
import com.theoryinpractise.halbuilder.spi.Link;
import com.theoryinpractise.halbuilder.spi.Representation;

import java.util.List;
import java.util.Map;

public class ImmutableRepresentation extends BaseRepresentation {

    private final Optional<Link> resourceLink;

    public ImmutableRepresentation(RepresentationFactory representationFactory,
                                   Map<String, String> namespaces, List<Link> links, Map<String, Optional<Object>> properties, Multimap<String, Representation> resources, boolean hasNullProperties) {
        super(representationFactory);
        this.namespaces = namespaces;
        this.links = links;
        this.properties = properties;
        this.resources = resources;

        this.resourceLink = super.getResourceLink();

        this.hasNullProperties = hasNullProperties;
    }

    public Optional<Link> getResourceLink() {
        return resourceLink;
    }


}
