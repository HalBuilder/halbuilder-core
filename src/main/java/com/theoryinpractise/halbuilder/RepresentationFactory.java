package com.theoryinpractise.halbuilder;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.theoryinpractise.halbuilder.impl.ContentType;
import com.theoryinpractise.halbuilder.impl.api.RepresentationReader;
import com.theoryinpractise.halbuilder.impl.json.JsonRenderer;
import com.theoryinpractise.halbuilder.impl.json.JsonRepresentationReader;
import com.theoryinpractise.halbuilder.impl.representations.MutableRepresentation;
import com.theoryinpractise.halbuilder.impl.xml.XmlRenderer;
import com.theoryinpractise.halbuilder.impl.xml.XmlRepresentationReader;
import com.theoryinpractise.halbuilder.spi.Link;
import com.theoryinpractise.halbuilder.spi.ReadableRepresentation;
import com.theoryinpractise.halbuilder.spi.Renderer;
import com.theoryinpractise.halbuilder.spi.Representation;
import com.theoryinpractise.halbuilder.spi.RepresentationException;

import java.io.BufferedReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.lang.String.format;

public class RepresentationFactory {
    public static final String HAL_XML = "application/hal+xml";
    public static final String HAL_JSON = "application/hal+json";

    private Map<ContentType, Class<? extends Renderer>> contentRenderers = Maps.newHashMap();
    private Map<ContentType, Class<? extends RepresentationReader>> resourceReaders = Maps.newHashMap();
    private TreeMap<String, String> namespaces = Maps.newTreeMap(Ordering.usingToString());
    private List<Link> links = Lists.newArrayList();
    private String baseHref;

    public RepresentationFactory() {
        this("http://localhost");
    }

    public RepresentationFactory(URI baseUri) {
        this(baseUri.toASCIIString());
    }

    public RepresentationFactory(String baseHref) {
        this.baseHref = baseHref;
        this.contentRenderers.put(new ContentType(HAL_XML), XmlRenderer.class);
        this.contentRenderers.put(new ContentType(HAL_JSON), JsonRenderer.class);
        this.resourceReaders.put(new ContentType(HAL_XML), XmlRepresentationReader.class);
        this.resourceReaders.put(new ContentType(HAL_JSON), JsonRepresentationReader.class);
    }

    public String getBaseHref() {
        return baseHref;
    }

    public RepresentationFactory withRenderer(String contentType, Class<? extends Renderer<String>> rendererClass) {
        contentRenderers.put(new ContentType(contentType), rendererClass);
        return this;
    }

    public RepresentationFactory withReader(String contentType, Class<? extends RepresentationReader> readerClass) {
        resourceReaders.put(new ContentType(contentType), readerClass);
        return this;
    }

    public RepresentationFactory withNamespace(String namespace, String url) {
        if (namespaces.containsKey(namespace)) {
            throw new RepresentationException(format("Duplicate namespace '%s' found for representation factory", namespace));
        }
        namespaces.put(namespace, url);
        return this;
    }

    public RepresentationFactory withLink(String url, String rel) {
        links.add(new Link(this, url, rel));
        return this;
    }

    public Representation newResource(URI uri) {
        return newResource(uri.toASCIIString());
    }

    public Representation newResource() {
        return newResource((String) null);
    }

    public Representation newResource(String href) {
        MutableRepresentation resource = new MutableRepresentation(this, href);

        // Add factory standard namespaces
        for (Map.Entry<String, String> entry : namespaces.entrySet()) {
            resource.withNamespace(entry.getKey(), entry.getValue());
        }

        // Add factory standard links
        for (Link link : links) {
            resource.withLink(link.getRel(), link.getHref(),
                              Optional.<Predicate<ReadableRepresentation>>absent(), link.getName(), link.getTitle(), link.getHreflang());
        }

        return resource;
    }

    public ReadableRepresentation readResource(Reader reader) {
        try {
            Reader bufferedReader =  new BufferedReader(reader);
            bufferedReader.mark(1);
            char firstChar = (char) bufferedReader.read();
            bufferedReader.reset();

            Class<? extends RepresentationReader> readerClass;
            switch (firstChar) {
            case '{':
                readerClass = resourceReaders.get(new ContentType(HAL_JSON));
                break;
            case '<':
                readerClass = resourceReaders.get(new ContentType(HAL_XML));
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

    public Renderer<String> lookupRenderer(String contentType) {

        for (Map.Entry<ContentType, Class<? extends Renderer>> entry : contentRenderers.entrySet()) {
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


}
