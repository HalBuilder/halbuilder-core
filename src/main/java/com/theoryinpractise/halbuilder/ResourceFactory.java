package com.theoryinpractise.halbuilder;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.theoryinpractise.halbuilder.impl.json.JsonResourceReader;
import com.theoryinpractise.halbuilder.impl.resources.MutableResource;
import com.theoryinpractise.halbuilder.impl.xml.XmlResourceReader;
import com.theoryinpractise.halbuilder.spi.Link;
import com.theoryinpractise.halbuilder.spi.ReadableResource;
import com.theoryinpractise.halbuilder.spi.Resource;
import com.theoryinpractise.halbuilder.spi.ResourceException;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.lang.String.format;

public class ResourceFactory {

    public static final Splitter WHITESPACE_SPLITTER = Splitter.onPattern("\\s")
                                                               .omitEmptyStrings();

    private TreeMap<String, String> namespaces = Maps.newTreeMap(Ordering.usingToString());
    private List<Link> links = Lists.newArrayList();
    private String baseHref;

    public ResourceFactory() {
        this.baseHref = "http://localhost";
    }

    public ResourceFactory(String baseHref) {
        this.baseHref = baseHref;
    }

    public String getBaseHref() {
        return baseHref;
    }

    public ResourceFactory withNamespace(String namespace, String url) {
        if (namespaces.containsKey(namespace)) {
            throw new ResourceException(format("Duplicate namespace '%s' found for resource factory", namespace));
        }
        namespaces.put(namespace, url);
        return this;
    }

    public ResourceFactory withLink(String url, String rel) {
        links.add(new Link(url, rel));
        return this;
    }

    public Resource newHalResource(String href) {
        MutableResource resource = new MutableResource(this, href);

        // Add factory standard namespaces
        for (Map.Entry<String, String> entry : namespaces.entrySet()) {
            resource.withNamespace(entry.getKey(), entry.getValue());
        }

        // Add factory standard links
        for (Link link : links) {
            resource.withLink(link.getHref(), link.getRel(), link.getName(), link.getTitle(), link.getHreflang());
        }

        return resource;
    }


    public ReadableResource newHalResource(Reader reader) {
        try {
            BufferedReader bufferedReader = new BufferedReader(reader);
            bufferedReader.mark(1);
            char firstChar = (char) bufferedReader.read();
            bufferedReader.reset();

            if (firstChar == '<') {
                return new XmlResourceReader(this).read(bufferedReader);
            } else if (firstChar == '{') {
                return new JsonResourceReader(this).read(bufferedReader);
            } else {
                throw new ResourceException("Unknown resource format");
            }
        } catch (Exception e) {
            throw new ResourceException(e);
        }
    }

}
