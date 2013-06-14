package com.theoryinpractise.halbuilder.impl.representations;

import com.google.common.collect.ImmutableMultimap;
import com.theoryinpractise.halbuilder.api.Link;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ImmutableRepresentation extends BaseRepresentation {

    private final Link resourceLink;

    public ImmutableRepresentation(RepresentationFactory representationFactory,
                                   NamespaceManager namespaceManager, List<Link> links, Map<String, Object> properties, Collection<Map.Entry<String, ReadableRepresentation>> resources, boolean hasNullProperties) {
        super(representationFactory);
        this.namespaceManager = namespaceManager;
        this.links = links;
        this.properties = properties;

        ImmutableMultimap.Builder<String, ReadableRepresentation> resourceBuilder = ImmutableMultimap.builder();
        for (Map.Entry<String, ReadableRepresentation> entry : resources) {
            resourceBuilder.putAll(entry.getKey(), entry.getValue());
        }
        this.resources = resourceBuilder.build();
        this.resourceLink = super.getResourceLink();
        this.hasNullProperties = hasNullProperties;
    }

    public Link getResourceLink() {
        return resourceLink;
    }


}
