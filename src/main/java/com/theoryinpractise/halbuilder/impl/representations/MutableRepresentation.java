package com.theoryinpractise.halbuilder.impl.representations;

import com.theoryinpractise.halbuilder.AbstractRepresentationFactory;
import com.theoryinpractise.halbuilder.api.*;
import com.theoryinpractise.halbuilder.impl.api.Support;
import fj.data.TreeMap;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URI;

import static fj.data.Option.fromNull;
import static java.lang.String.format;

public class MutableRepresentation
    extends BaseRepresentation
    implements Representation {

  public MutableRepresentation(AbstractRepresentationFactory representationFactory, String href) {
    super(representationFactory);
    if (href != null) {
      this.links = this.links.cons(new Link(representationFactory, "self", href));
    }
  }

  public MutableRepresentation(AbstractRepresentationFactory representationFactory) {
    super(representationFactory);
  }

  /**
   * Define rel semantics for this representation
   *
   * @param rel A defined relationship type
   */
  public MutableRepresentation withRel(Rel rel) {
    if (rels.contains(rel.rel())) {
      throw new IllegalStateException(String.format("Rel %s is already declared.", rel.rel()));
    }
    rels = rels.set(rel.rel(), rel);
    return this;
  }

  /**
   * Add a link to this resource
   *
   * @param rel
   * @param href The target href for the link, relative to the href of this resource.
   * @return
   */
  public MutableRepresentation withLink(String rel, String href) {
    withLink(rel, href, null, null, null, null);
    return this;
  }

  /**
   * Add a link to this resource
   *
   * @param rel
   * @param uri The target URI for the link, possibly relative to the href of
   *            this resource.
   * @return
   */
  public MutableRepresentation withLink(String rel, URI uri) {
    return withLink(rel, uri.toASCIIString());
  }

  /**
   * Add a link to this resource
   *
   * @param rel
   * @param href The target href for the link, relative to the href of this resource.
   */
  public MutableRepresentation withLink(String rel, String href, String name, String title, String hreflang, String profile) {
    Support.checkRelType(rel);
    validateSingletonRel(rel);
    if (!rels.contains(rel)) {
      withRel(Rel.natural(rel));
    }

    links = links.cons(new Link(representationFactory, rel, href, name, title, hreflang, profile));
    return this;
  }

  public Representation withProperty(String name, Object value) {
    if (properties.contains(name)) {
      throw new RepresentationException(format("Duplicate property '%s' found for resource", name));
    }
    if (null == value) {
      this.hasNullProperties = true;
    }
    properties = properties.set(name, fromNull(value));
    return this;
  }

  public Representation withBean(Object value) {
    try {
      BeanInfo beanInfo = Introspector.getBeanInfo(value.getClass());
      for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
        if (!"class".equals(pd.getName())) {
          withProperty(pd.getName(), pd.getReadMethod().invoke(value));
        }
      }
    } catch (IntrospectionException | InvocationTargetException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  public Representation withFields(Object value) {
    try {
      for (Field field : value.getClass().getDeclaredFields()) {
        if (Modifier.isPublic(field.getModifiers())) {
          withProperty(field.getName(), field.get(value));
        }
      }
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  public Representation withRepresentable(Representable representable) {
    representable.representResource(this);
    return this;
  }

  public Representation withFieldBasedRepresentation(String rel, String href, Object o) {
    return withRepresentation(rel, representationFactory.newRepresentation(href).withFields(o));
  }

  public Representation withBeanBasedRepresentation(String rel, String href, Object o) {
    return withRepresentation(rel, representationFactory.newRepresentation(href).withBean(o));
  }

  /**
   * Adds a new namespace
   *
   * @param namespace
   * @param href      The target href of the namespace being added. This may be relative to the resourceFactories baseref
   * @return
   */
  public Representation withNamespace(String namespace, String href) {
    if (!rels.contains("curies")) {
      rels = rels.set("curies", Rel.natural("curies"));
    }

    namespaceManager.withNamespace(namespace, href);
    return this;
  }

  public MutableRepresentation withRepresentation(String rel, ReadableRepresentation resource) {
    Support.checkRelType(rel);
    validateSingletonRel(rel);
    if (!rels.contains(rel)) {
      withRel(Rel.natural(rel));
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
      // Rel is resisted, check for duplicate singleton
      if (rel.isSingleton() && (!getLinksByRel(rel.rel()).isEmpty() || !getResourcesByRel(rel.rel()).isEmpty())) {
        throw new IllegalStateException(String.format("%s is registered as a single rel and already exists.", rel));
      }
    });
  }

  /**
   * Retrieve the defined rel semantics for this representation
   *
   * @return
   */
  public TreeMap<String, Rel> getRels() {
    return rels;
  }

}
