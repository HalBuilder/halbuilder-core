package com.theoryinpractise.halbuilder;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.theoryinpractise.halbuilder.api.Link;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.Representation;
import com.theoryinpractise.halbuilder.api.RepresentationException;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.api.RepresentationReader;
import com.theoryinpractise.halbuilder.api.RepresentationWriter;
import com.theoryinpractise.halbuilder.impl.ContentType;
import com.theoryinpractise.halbuilder.impl.representations.MutableRepresentation;
import com.theoryinpractise.halbuilder.impl.representations.NamespaceManager;

import java.io.BufferedReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultRepresentationFactory extends RepresentationFactory {

    private Map<ContentType, Class<? extends RepresentationWriter>> contentRenderers = Maps.newHashMap();
    private Map<ContentType, Class<? extends RepresentationReader>> representationReaders = Maps.newHashMap();
    private NamespaceManager namespaceManager = new NamespaceManager();
    private List<Link> links = Lists.newArrayList();
    private Set<URI> flags = Sets.newHashSet();

    public DefaultRepresentationFactory withRenderer(String contentType, Class<? extends RepresentationWriter<String>> rendererClass) {
        contentRenderers.put(new ContentType(contentType), rendererClass);
        return this;
    }

    public DefaultRepresentationFactory withReader(String contentType, Class<? extends RepresentationReader> readerClass) {
        representationReaders.put(new ContentType(contentType), readerClass);
        return this;
    }

    @Override
    public DefaultRepresentationFactory withNamespace(String namespace, String href) {
        namespaceManager.withNamespace(namespace, href);
        return this;
    }

    @Override
    public DefaultRepresentationFactory withLink(String rel, String href) {
        links.add(new Link(this, rel, href));
        return this;
    }

    @Override
    public RepresentationFactory withFlag(URI flag) {
        flags.add(flag);
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
        for (Map.Entry<String, String> entry : namespaceManager.getNamespaces().entrySet()) {
            representation.withNamespace(entry.getKey(), entry.getValue());
        }

        // Add factory standard links
        for (Link link : links) {
            representation.withLink(link.getRel(), link.getHref(), link.getName(), link.getTitle(), link.getHreflang(), link.getProfile());
        }

        return representation;
    }

    @Override
    public ReadableRepresentation readRepresentation(Reader reader) {
        try {
            Reader bufferedReader = new BufferedReader(reader);
            bufferedReader.mark(1);
            char firstChar = (char) bufferedReader.read();
            bufferedReader.reset();

            Class<? extends RepresentationReader> readerClass;
            switch (firstChar) {
                case '{':
                    readerClass = representationReaders.get(new ContentType(HAL_JSON));
                    break;
                case '<':
                    readerClass = representationReaders.get(new ContentType(HAL_XML));
                    break;
                default:
                    throw new RepresentationException("unrecognized initial character in stream: " + firstChar);
            }
            Constructor<? extends RepresentationReader> readerConstructor = readerClass.getConstructor(RepresentationFactory.class);
            return readerConstructor.newInstance(this).read(bufferedReader);
        } catch (Exception e) {
            throw new RepresentationException(e);
        }
    }

    public RepresentationWriter<String> lookupRenderer(String contentType) {

        for (Map.Entry<ContentType, Class<? extends RepresentationWriter>> entry : contentRenderers.entrySet()) {
            if (entry.getKey().matches(contentType)) {
                try {
                    return entry.getValue().newInstance();
                } catch (InstantiationException e) {
                    throw new RepresentationException(e);
                } catch (IllegalAccessException e) {
                    throw new RepresentationException(e);
                }
            }
        }

        throw new IllegalArgumentException("Unsupported contentType: " + contentType);

    }

    public Set<URI> getFlags() {
        return ImmutableSet.copyOf(flags);
    }

}
