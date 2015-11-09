package com.theoryinpractise.halbuilder.impl.representations;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.theoryinpractise.halbuilder.AbstractRepresentationFactory;
import com.theoryinpractise.halbuilder.api.Link;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.Rel;
import com.theoryinpractise.halbuilder.api.Rels;
import com.theoryinpractise.halbuilder.api.Representable;
import com.theoryinpractise.halbuilder.api.Representation;
import com.theoryinpractise.halbuilder.api.RepresentationException;
import com.theoryinpractise.halbuilder.impl.api.Support;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.collection.TreeMap;
import javaslang.control.Option;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URI;

import static java.lang.String.format;
import static java.util.Comparator.naturalOrder;

public class PersistentRepresentation
    extends BaseRepresentation
    implements Representation {

  public PersistentRepresentation(AbstractRepresentationFactory representationFactory) {
    this(representationFactory, null);
  }

  public PersistentRepresentation(AbstractRepresentationFactory representationFactory, String href) {
    super(representationFactory, Option.none());
    if (href != null) {
      this.links = this.links.append(new Link("self", href));
    }
  }

  public PersistentRepresentation(final AbstractRepresentationFactory representationFactory,
                                  final Option<String> content,
                                  final List<Link> links,
                                  final TreeMap<String, Rel> rels,
                                  final NamespaceManager namespaceManager,
                                  final TreeMap<String, Option<Object>> properties,
                                  final Multimap<String, ReadableRepresentation> resources) {
    super(representationFactory, content);
    this.links = links;
    this.rels = rels;
    this.namespaceManager = namespaceManager;
    this.properties = properties;
    this.resources = resources;
  }

  public static PersistentRepresentation empty(final AbstractRepresentationFactory representationFactory) {
    return new PersistentRepresentation(representationFactory,
                                           Option.none(),
                                           List.empty(),
                                           TreeMap.empty(naturalOrder()),
                                           NamespaceManager.EMPTY,
                                           TreeMap.empty(naturalOrder()),
                                           ArrayListMultimap.create());
  }

  /**
   * Retrieve the defined rel semantics for this representation.
   *
   * @return
   */
  public Map<String, Rel> getRels() {
    return rels;
  }

  /**
   * Adds or replaces the content of the representation.
   *
   * @param content The source content of the representation.
   *
   * @return A new instance of a PersistentRepresentation with the namespace included.
   */
  public PersistentRepresentation withContent(String content) {
    return new PersistentRepresentation(representationFactory, Option.of(content), links, rels,
                                           namespaceManager, properties, resources);
  }

  /**
   * Define rel semantics for this representation.
   *
   * @param rel A defined relationship type
   */
  public PersistentRepresentation withRel(Rel rel) {
    if (rels.containsKey(rel.rel())) {
      throw new IllegalStateException(String.format("Rel %s is already declared.", rel.rel()));
    }
    final TreeMap<String, Rel> updatedRels = rels.put(rel.rel(), rel);
    return new PersistentRepresentation(representationFactory, content, links, updatedRels,
                                           namespaceManager, properties, resources);
  }

  /**
   * Add a link to this resource.
   *
   * @param rel
   * @param href The target href for the link, relative to the href of this resource.
   *
   * @return
   */
  public PersistentRepresentation withLink(String rel, String href) {
    return withLink(rel, href, null, null, null, null);
  }

  /**
   * Add a link to this resource.
   *
   * @param rel
   * @param uri The target URI for the link, possibly relative to the href of this resource.
   *
   * @return
   */
  public PersistentRepresentation withLink(String rel, URI uri) {
    return withLink(rel, uri.toASCIIString());
  }

  /**
   * Add a link to this resource.
   *
   * @param rel
   * @param href The target href for the link, relative to the href of this resource.
   */
  public PersistentRepresentation withLink(String rel, String href, String name, String title,
                                           String hreflang, String profile) {
    Support.checkRelType(rel);
    validateSingletonRel(rel);
    if (!rels.containsKey(rel)) {
      withRel(Rels.natural(rel));
    }

    final List<Link> updatedLinks = links.append(new Link(rel, href, name, title, hreflang, profile));
    return new PersistentRepresentation(representationFactory, content, updatedLinks, rels,
                                           namespaceManager, properties, resources);
  }

  public PersistentRepresentation withProperty(String name, Object value) {
    if (properties.containsKey(name)) {
      throw new RepresentationException(format("Duplicate property '%s' found for resource", name));
    }
    if (null == value) {
      this.hasNullProperties = true;
    }
    final TreeMap<String, Option<Object>> updateProperties = properties.put(name, Option.of(value));
    return new PersistentRepresentation(representationFactory, content, links, rels, namespaceManager,
                                           updateProperties, resources);
  }

  public PersistentRepresentation withProperties(Map<String, Object> properties) {
    return properties.foldLeft(this,
        (rep, entry) -> rep.withProperty(entry._1, entry._2));
  }

  public PersistentRepresentation withBean(Object value) {
    PersistentRepresentation updatedRepresentation = this;
    try {
      BeanInfo beanInfo = Introspector.getBeanInfo(value.getClass());
      for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
        if (!"class".equals(pd.getName())) {
          updatedRepresentation = updatedRepresentation.withProperty(pd.getName(), pd.getReadMethod().invoke(value));
        }
      }
    } catch (IntrospectionException | InvocationTargetException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    return updatedRepresentation;
  }

  public PersistentRepresentation withFields(Object value) {
    PersistentRepresentation updatedRepresentation = this;
    try {
      for (Field field : value.getClass().getDeclaredFields()) {
        if (Modifier.isPublic(field.getModifiers())) {
          updatedRepresentation = updatedRepresentation.withProperty(field.getName(), field.get(value));
        }
      }
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    return updatedRepresentation;
  }

  public Representation withRepresentable(Representable representable) {
    return representable.representResource(this);
  }

  public PersistentRepresentation withFieldBasedRepresentation(String rel, String href, Object o) {
    return withRepresentation(rel, representationFactory.newRepresentation(href).withFields(o));
  }

  public PersistentRepresentation withBeanBasedRepresentation(String rel, String href, Object o) {
    return withRepresentation(rel, representationFactory.newRepresentation(href).withBean(o));
  }

  /**
   * Adds a new namespace.
   *
   * @param namespace The CURIE prefix for the namespace being added.
   * @param href      The target href of the namespace being added. This may be relative to the resourceFactories baseref
   *
   * @return A new instance of a PersistentRepresentation with the namespace included.
   */
  public PersistentRepresentation withNamespace(String namespace, String href) {
    if (!rels.containsKey("curies")) {
      rels = rels.put("curies", Rels.natural("curies"));
    }

    final NamespaceManager updatedNamespaceManager = namespaceManager.withNamespace(namespace, href);
    return new PersistentRepresentation(representationFactory, content, links, rels,
                                           updatedNamespaceManager, properties, resources);
  }

  public PersistentRepresentation withRepresentation(String rel, ReadableRepresentation resource) {
    Support.checkRelType(rel);
    validateSingletonRel(rel);
    if (!rels.containsKey(rel)) {
      withRel(Rels.natural(rel));
    }

    resources.put(rel, resource);
    // Propagate null property flag to parent.
    if (resource.hasNullProperties()) {
      hasNullProperties = true;
    }
    return this;
  }

  private void validateSingletonRel(String unvalidatedRel) {
    rels.get(unvalidatedRel).forEach(rel -> {
      // Rel is register, check for duplicate singleton
      if (isSingleton(rel)
          && (!getLinksByRel(rel).isEmpty() || !getResourcesByRel(rel).isEmpty())) {
        throw new IllegalStateException(String.format(
            "%s is registered as a single rel and already exists.", rel));
      }
    });
  }

  private static boolean isSingleton(Rel rel) {
    return rel.match(Rels.cases(
        (__) -> true,
        (__) -> false,
        (__, id, comparator) -> false
    ));
  }

}
