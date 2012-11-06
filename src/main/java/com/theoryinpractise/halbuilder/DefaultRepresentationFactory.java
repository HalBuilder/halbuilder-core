package com.theoryinpractise.halbuilder;

import com.google.common.collect.*;
import com.theoryinpractise.halbuilder.api.*;
import com.theoryinpractise.halbuilder.impl.ContentType;
import com.theoryinpractise.halbuilder.impl.json.JsonRepresentationReader;
import com.theoryinpractise.halbuilder.impl.json.JsonRepresentationWriter;
import com.theoryinpractise.halbuilder.impl.representations.MutableRepresentation;
import com.theoryinpractise.halbuilder.impl.xml.XmlRepresentationReader;
import com.theoryinpractise.halbuilder.impl.xml.XmlRepresentationWriter;

import java.io.BufferedReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static java.lang.String.format;

public class DefaultRepresentationFactory extends RepresentationFactory {

    private Map<ContentType, Class<? extends RepresentationWriter>> contentRenderers = Maps.newHashMap();
    private Map<ContentType, Class<? extends RepresentationReader>> representationReaders = Maps.newHashMap();
    private TreeMap<String, String> namespaces = Maps.newTreeMap(Ordering.usingToString());
    private List<Link> links = Lists.newArrayList();
    private Set<URI> flags = Sets.newHashSet();

    public DefaultRepresentationFactory() {
        this.contentRenderers.put(new ContentType(HAL_XML), XmlRepresentationWriter.class);
        this.contentRenderers.put(new ContentType(HAL_JSON), JsonRepresentationWriter.class);
        this.representationReaders.put(new ContentType(HAL_XML), XmlRepresentationReader.class);
        this.representationReaders.put(new ContentType(HAL_JSON), JsonRepresentationReader.class);
    }

    public DefaultRepresentationFactory withRenderer(String contentType, Class<? extends RepresentationWriter<String>> rendererClass) {
        contentRenderers.put(new ContentType(contentType), rendererClass);
        return this;
    }

    public DefaultRepresentationFactory withReader(String contentType, Class<? extends RepresentationReader> readerClass) {
        representationReaders.put(new ContentType(contentType), readerClass);
        return this;
    }

    @Override
    public DefaultRepresentationFactory withNamespace(String namespace, String url) {
        if (namespaces.containsKey(namespace)) {
            throw new RepresentationException(format("Duplicate namespace '%s' found for representation factory", namespace));
        }
        namespaces.put(namespace, url);
        return this;
    }

    @Override
    public DefaultRepresentationFactory withLink(String rel, String url) {
        links.add(new Link(this, rel, url));
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
        for (Map.Entry<String, String> entry : namespaces.entrySet()) {
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
