package com.theoryinpractise.halbuilder.impl.representations;

import com.google.common.collect.ImmutableMultimap;
import com.theoryinpractise.halbuilder.AbstractRepresentationFactory;
import com.theoryinpractise.halbuilder.api.Link;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.Rel;
import fj.data.List;
import fj.data.Option;
import fj.data.TreeMap;

import java.util.Collection;
import java.util.Map;

public class ImmutableRepresentation
    extends BaseRepresentation {

  private final Link resourceLink;

  public ImmutableRepresentation(AbstractRepresentationFactory representationFactory,
                                 NamespaceManager namespaceManager, List<Link> links,
                                 TreeMap<String, Option<Object>> properties,
                                 Collection<Map.Entry<String, ReadableRepresentation>> resources,
                                 boolean hasNullProperties) {

    super(representationFactory);
    this.namespaceManager = namespaceManager;
    this.links = links;
    this.properties = properties;

    ImmutableMultimap.Builder<String, ReadableRepresentation> resourceBuilder = ImmutableMultimap.builder();

    resources.forEach(r -> resourceBuilder.putAll(r.getKey(), r.getValue()));

    this.resources = resourceBuilder.build();
    this.resourceLink = super.getResourceLink();
    this.hasNullProperties = hasNullProperties;
  }

  public Link getResourceLink() {
    return resourceLink;
  }

  public TreeMap<String, Rel> getRels() {
    return rels;
  }
}
