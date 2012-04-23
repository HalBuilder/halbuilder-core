package com.theoryinpractise.halbuilder;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.theoryinpractise.halbuilder.impl.ContentType;
import com.theoryinpractise.halbuilder.impl.api.ResourceReader;
import com.theoryinpractise.halbuilder.impl.json.JsonRenderer;
import com.theoryinpractise.halbuilder.impl.json.JsonResourceReader;
import com.theoryinpractise.halbuilder.impl.resources.MutableResource;
import com.theoryinpractise.halbuilder.impl.xml.XmlRenderer;
import com.theoryinpractise.halbuilder.impl.xml.XmlResourceReader;
import com.theoryinpractise.halbuilder.spi.Link;
import com.theoryinpractise.halbuilder.spi.ReadableResource;
import com.theoryinpractise.halbuilder.spi.Renderer;
import com.theoryinpractise.halbuilder.spi.Resource;
import com.theoryinpractise.halbuilder.spi.ResourceException;

import java.io.BufferedReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.lang.String.format;

public class ResourceFactory {
    public static final String HAL_XML = "application/hal+xml";
    public static final String HAL_JSON = "application/hal+json";

    private Map<ContentType, Class<? extends Renderer>> contentRenderers = Maps.newHashMap();
    private Map<ContentType, Class<? extends ResourceReader>> resourceReaders = Maps.newHashMap();
    private TreeMap<String, String> namespaces = Maps.newTreeMap(Ordering.usingToString());
    private List<Link> links = Lists.newArrayList();
    private String baseHref;

    public ResourceFactory() {
        this("http://localhost");
    }

    public ResourceFactory(URI baseUri) {
        this(baseUri.toASCIIString());
    }

    public ResourceFactory(String baseHref) {
        this.baseHref = baseHref;
        this.contentRenderers.put(new ContentType(HAL_XML), XmlRenderer.class);
        this.contentRenderers.put(new ContentType(HAL_JSON), JsonRenderer.class);
        this.resourceReaders.put(new ContentType(HAL_XML), XmlResourceReader.class);
        this.resourceReaders.put(new ContentType(HAL_JSON), JsonResourceReader.class);
    }

    public String getBaseHref() {
        return baseHref;
    }

    public ResourceFactory withRenderer(String contentType, Class<? extends Renderer<String>> rendererClass) {
        contentRenderers.put(new ContentType(contentType), rendererClass);
        return this;
    }

    public ResourceFactory withReader(String contentType, Class<? extends ResourceReader> readerClass) {
        resourceReaders.put(new ContentType(contentType), readerClass);
        return this;
    }

    public ResourceFactory withNamespace(String namespace, String url) {
        if (namespaces.containsKey(namespace)) {
            throw new ResourceException(format("Duplicate namespace '%s' found for resource factory", namespace));
        }
        namespaces.put(namespace, url);
        return this;
    }

    public ResourceFactory withLink(String url, String rel) {
        links.add(new Link(this, url, rel));
        return this;
    }

    public Resource newResource(URI uri) {
        return newResource(uri.toASCIIString());
    }

    public Resource newResource(String href) {
        MutableResource resource = new MutableResource(this, href);

        // Add factory standard namespaces
        for (Map.Entry<String, String> entry : namespaces.entrySet()) {
            resource.withNamespace(entry.getKey(), entry.getValue());
        }

        // Add factory standard links
        for (Link link : links) {
            resource.withLink(link.getHref(), link.getRel(),
                    Optional.<Predicate<ReadableResource>>absent(), link.getName(), link.getTitle(), link.getHreflang());
        }

        return resource;
    }

    public ReadableResource readResource(Reader reader) {
        try {
            Reader bufferedReader =  new BufferedReader(reader);
            bufferedReader.mark(1);
            char firstChar = (char) bufferedReader.read();
            bufferedReader.reset();

            Class<? extends ResourceReader> readerClass;
            switch (firstChar) {
            case '{':
                readerClass = resourceReaders.get(new ContentType(HAL_JSON));
                break;
            case '<':
                readerClass = resourceReaders.get(new ContentType(HAL_XML));
                break;
            default:
                throw new ResourceException("unrecognized initial character in stream: " + firstChar);
            }
            Constructor<? extends ResourceReader> readerConstructor = readerClass.getConstructor(ResourceFactory.class);
            return readerConstructor.newInstance(this).read(bufferedReader);
        } catch (Exception e) {
            throw new ResourceException(e);
        }
    }

    public Renderer<String> lookupRenderer(String contentType) {

        for (Map.Entry<ContentType, Class<? extends Renderer>> entry : contentRenderers.entrySet()) {
            if (entry.getKey().matches(contentType)) {
                try {
                    return entry.getValue().newInstance();
                } catch (InstantiationException e) {
                    throw new ResourceException(e);
                } catch (IllegalAccessException e) {
                    throw new ResourceException(e);
                }
            }
        }

        throw new IllegalArgumentException("Unsupported contentType: " + contentType);

    }


}
