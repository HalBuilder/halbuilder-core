package com.theoryinpractise.halbuilder;

import com.theoryinpractise.halbuilder.api.Link;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.Rel;
import com.theoryinpractise.halbuilder.api.Rels;
import com.theoryinpractise.halbuilder.api.Representation;
import com.theoryinpractise.halbuilder.api.RepresentationException;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.api.RepresentationReader;
import com.theoryinpractise.halbuilder.api.RepresentationWriter;
import com.theoryinpractise.halbuilder.impl.ContentType;
import com.theoryinpractise.halbuilder.impl.representations.NamespaceManager;
import com.theoryinpractise.halbuilder.impl.representations.PersistentRepresentation;
import javaslang.collection.HashMap;
import javaslang.collection.HashSet;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.collection.Set;
import javaslang.collection.TreeMap;

import java.io.Reader;
import java.net.URI;

import static java.lang.String.format;

public class DefaultRepresentationFactory
    extends AbstractRepresentationFactory {

  private Map<ContentType, Class<? extends RepresentationWriter<String>>> contentRenderers = HashMap.empty();
  private Map<ContentType, Class<? extends RepresentationReader>> representationReaders = HashMap.empty();
  private NamespaceManager namespaceManager = NamespaceManager.EMPTY;
  private List<Link> links = List.empty();
  private Set<URI> flags = HashSet.empty();
  private TreeMap<String, Rel> rels = TreeMap.empty();

  public DefaultRepresentationFactory withRenderer(String contentType,
                                                   Class<? extends RepresentationWriter<String>>
                                                       rendererClass) {
    contentRenderers = contentRenderers.put(new ContentType(contentType), rendererClass);
    return this;
  }

  public DefaultRepresentationFactory withReader(String contentType, Class<? extends
                                                                               RepresentationReader> readerClass) {
    representationReaders = representationReaders.put(new ContentType(contentType), readerClass);
    return this;
  }

  @Override
  public DefaultRepresentationFactory withNamespace(String namespace, String href) {
    namespaceManager = namespaceManager.withNamespace(namespace, href);
    return this;
  }

  @Override
  public RepresentationFactory withRel(Rel rel) {
    if (rels.containsKey(Rels.getRel(rel))) {
      throw new IllegalStateException(format("Rel %s is already declared.", rel.rel()));
    }
    rels = rels.put(rel.rel(), rel);
    return this;
  }

  @Override
  public DefaultRepresentationFactory withLink(String rel, String href) {
    links = links.append(new Link(rel, href));
    return this;
  }

  @Override
  public RepresentationFactory withFlag(URI flag) {
    flags = flags.add(flag);
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
    PersistentRepresentation representation = PersistentRepresentation.empty(this)
                                                                      .withRel(Rels.singleton("self"));

    if (href != null) {
      representation = representation.withLink("self", href);
    }

    // Add factory standard namespaces
    representation = namespaceManager.getNamespaces()
                                     .foldLeft(representation, (rep, ns) -> rep.withNamespace(ns._1, ns._2));

    // Add factory standard rels
    representation = rels.foldLeft(representation, (rep, rel) -> rep.withRel(rel._2));

    // Add factory standard links

    return links.foldLeft(representation,
        (rep, link) -> rep.withLink(link.getRel(),
            link.getHref(),
            link.getName(),
            link.getTitle(),
            link.getHreflang(),
            link.getProfile()));
  }

  @Override
  public ReadableRepresentation readRepresentation(String contentType, Reader reader) {
    try {
      return representationReaders.get(new ContentType(contentType))
                                  .map(rc -> buildContentRepresentation(rc, reader))
                                  .orElseThrow(() -> {
                                    throw new IllegalStateException(format(
                                        "No representation reader for content type %s registered.", contentType));
                                  });

    } catch (Exception e) {
      throw new RepresentationException(e);
    }
  }

  private ReadableRepresentation buildContentRepresentation(Class<? extends RepresentationReader>
                                                                rc, Reader reader) {
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

    return contentRenderers.findFirst(ct -> ct._1.matches(contentType))
                           .map(ct -> newInstanceOf(ct._2))
                           .orElseThrow(() -> {
                             throw new IllegalArgumentException("Unsupported contentType: " + contentType);
                           });

  }

  public RepresentationWriter<String> newInstanceOf(final Class<? extends
                                                                    RepresentationWriter<String>>
                                                        writerClass) {
    try {
      return writerClass.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RepresentationException(e);
    }
  }

}
