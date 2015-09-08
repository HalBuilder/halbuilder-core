package com.theoryinpractise.halbuilder;

import com.theoryinpractise.halbuilder.api.*;
import com.theoryinpractise.halbuilder.impl.ContentType;
import com.theoryinpractise.halbuilder.impl.representations.MutableRepresentation;
import com.theoryinpractise.halbuilder.impl.representations.NamespaceManager;
import fj.data.List;
import fj.data.Set;
import fj.data.TreeMap;

import java.io.Reader;
import java.net.URI;

import static fj.Ord.hashOrd;
import static fj.Ord.stringOrd;
import static java.lang.String.format;

public class DefaultRepresentationFactory
    extends AbstractRepresentationFactory {

  private TreeMap<ContentType, Class<? extends RepresentationWriter>> contentRenderers      = TreeMap.empty(hashOrd());
  private TreeMap<ContentType, Class<? extends RepresentationReader>> representationReaders = TreeMap.empty(hashOrd());
  private NamespaceManager                                            namespaceManager      = new NamespaceManager();
  private List<Link>                                                  links                 = List.nil();
  private Set<URI>                                                    flags                 = Set.empty(hashOrd());
  private TreeMap<String, Rel>                                        rels                  = TreeMap.empty(stringOrd);

  public DefaultRepresentationFactory() {
    withRel(Rel.singleton("self"));
  }

  public DefaultRepresentationFactory withRenderer(String contentType,
                                                   Class<? extends RepresentationWriter<String>> rendererClass) {
    contentRenderers = contentRenderers.set(new ContentType(contentType), rendererClass);
    return this;
  }

  public DefaultRepresentationFactory withReader(String contentType, Class<? extends RepresentationReader> readerClass) {
    representationReaders = representationReaders.set(new ContentType(contentType), readerClass);
    return this;
  }

  @Override
  public DefaultRepresentationFactory withNamespace(String namespace, String href) {
    namespaceManager.withNamespace(namespace, href);
    return this;
  }

  @Override
  public RepresentationFactory withRel(Rel rel) {
    if (rels.contains(rel.rel())) {
      throw new IllegalStateException(format("Rel %s is already declared.", rel.rel()));
    }
    rels = rels.set(rel.rel(), rel);
    return this;
  }

  @Override
  public DefaultRepresentationFactory withLink(String rel, String href) {
    links = links.cons(new Link(this, rel, href));
    return this;
  }

  @Override
  public RepresentationFactory withFlag(URI flag) {
    flags = flags.insert(flag);
    return this;
  }

  @Override
  public Representation newRepresentation(URI uri) {
    return newRepresentation(uri.toString());
  }

  @Override
  public Representation newRepresentation() {
    return newRepresentation((String) null);
  }

  @Override
  public Representation newRepresentation(String href) {
    MutableRepresentation representation = new MutableRepresentation(this, href);

    // Add factory standard namespaces
    namespaceManager.getNamespaces().forEach(ns -> representation.withNamespace(ns._1(), ns._2()));

    // Add factorry standard rels
    for (Rel rel : rels.values()) {
      representation.withRel(rel);
    }

    // Add factory standard links
    for (Link link : links) {
      representation.withLink(link.getRel(), link.getHref(), link.getName(), link.getTitle(), link.getHreflang(), link.getProfile
                                                                                                                           ());
    }

    return representation;
  }

  @Override
  public ContentRepresentation readRepresentation(String contentType, Reader reader) {
    try {
      return representationReaders.get(new ContentType(contentType))
                                  .map(rc -> buildContentRepresentation(rc, reader))
                                  .orSome(() -> {
                                    throw new IllegalStateException(format("No representation reader for content type %s "
                                                                           + "registered.", contentType));
                                  });

    } catch (Exception e) {
      throw new RepresentationException(e);
    }
  }

  private ContentRepresentation buildContentRepresentation(Class<? extends RepresentationReader> rc, Reader reader) {
    try {
      return rc.getConstructor(AbstractRepresentationFactory.class).newInstance(this).read(reader);
    } catch (ReflectiveOperationException e) {
      throw new RepresentationException(e);
    }
  }

  public Set<URI> getFlags() {
    return flags;
  }

  public TreeMap<String, Rel> getRels() {
    return rels;
  }

  public RepresentationWriter<String> lookupRenderer(String contentType) {

    return contentRenderers.toStream()
                           .find(ct -> ct._1().matches(contentType))
                           .map(ct -> newInstanceOf(ct._2()))
                           .orSome(() -> {
                             throw new IllegalArgumentException("Unsupported contentType: " + contentType);
                           });

  }

  public RepresentationWriter newInstanceOf(final Class<? extends RepresentationWriter> writerClass) {
    try {
      return writerClass.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RepresentationException(e);
    }
  }

}
