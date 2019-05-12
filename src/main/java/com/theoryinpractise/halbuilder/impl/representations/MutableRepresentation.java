package com.theoryinpractise.halbuilder.impl.representations;

import static java.lang.String.format;

import com.google.common.base.CaseFormat;
import com.google.common.base.Optional;
import com.theoryinpractise.halbuilder.AbstractRepresentationFactory;
import com.theoryinpractise.halbuilder.api.Link;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.Representable;
import com.theoryinpractise.halbuilder.api.Representation;
import com.theoryinpractise.halbuilder.api.RepresentationException;
import com.theoryinpractise.halbuilder.impl.api.Support;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

public class MutableRepresentation extends BaseRepresentation implements Representation {

  public MutableRepresentation(AbstractRepresentationFactory representationFactory, String href) {
    super(representationFactory);
    if (href != null && !"".equals(href)) {
      this.links.add(new Link(representationFactory, "self", href));
    }
  }

  public MutableRepresentation(AbstractRepresentationFactory representationFactory) {
    super(representationFactory);
  }

  /**
   * Add a link to this resource
   *
   * @param rel
   * @param href The target href for the link, relative to the href of this resource.
   * @return
   */
  @Override
  public MutableRepresentation withLink(String rel, String href) {
    withLink(rel, href, "", "", "", "");
    return this;
  }

  /**
   * Add a link to this resource
   *
   * @param rel
   * @param href The target href for the link, relative to the href of this resource.
   */
  @Override
  public MutableRepresentation withLink(
      String rel, String href, String name, String title, String hreflang, String profile) {
    Support.checkRelType(rel);
    links.add(new Link(representationFactory, rel, href, name, title, hreflang, profile));
    return this;
  }

  /**
   * Add a link to this resource
   *
   * @param rel
   * @param uri The target URI for the link, possibly relative to the href of this resource.
   * @return
   */
  @Override
  public MutableRepresentation withLink(String rel, URI uri) {
    return withLink(rel, uri.toASCIIString());
  }

  @Override
  public Representation withProperty(String name, @Nullable Object value) {
    if (properties.containsKey(name)) {
      throw new RepresentationException(format("Duplicate property '%s' found for resource", name));
    }
    if (null == value) {
      this.hasNullProperties = true;
    }
    properties.put(name, value);
    return this;
  }

  private static Pattern propertyReadMethod = Pattern.compile("^(get|is|has)(.+)$");

  private static Pattern ignoredMethod = Pattern.compile("^(getClass|hashCode|equals)$");

  static Optional<String> findPropertyReadMethod(String methodName) {
    if (!ignoredMethod.matcher(methodName).matches()) {
      Matcher matcher = propertyReadMethod.matcher(methodName);
      if (matcher.matches()) {
        return Optional.of(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, matcher.group(2)));
      }
    }
    return Optional.absent();
  }

  @Override
  public Representation withBean(Object value) {
    try {
      Method[] methods = value.getClass().getMethods();
      for (Method method : methods) {
        Optional<String> propertyReader = findPropertyReadMethod(method.getName());
        if (propertyReader.isPresent()) {
          withProperty(propertyReader.get(), method.invoke(value));
        }
      }
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  @Override
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

  @Override
  public Representation withRepresentable(Representable representable) {
    representable.representResource(this);
    return this;
  }

  @Override
  public Representation withFieldBasedRepresentation(String rel, String href, Object o) {
    return withRepresentation(rel, representationFactory.newRepresentation(href).withFields(o));
  }

  @Override
  public Representation withBeanBasedRepresentation(String rel, String href, Object o) {
    return withRepresentation(rel, representationFactory.newRepresentation(href).withBean(o));
  }

  /**
   * Adds a new namespace
   *
   * @param namespace
   * @param href The target href of the namespace being added. This may be relative to the
   *     resourceFactories baseref
   * @return
   */
  @Override
  public Representation withNamespace(String namespace, String href) {
    namespaceManager.withNamespace(namespace, href);
    return this;
  }

  @Override
  public MutableRepresentation withRepresentation(String rel, ReadableRepresentation resource) {
    Support.checkRelType(rel);
    resources.put(rel, resource);
    // Propagate null property flag to parent.
    if (resource.hasNullProperties()) {
      hasNullProperties = true;
    }
    return this;
  }
}
