package com.theoryinpractise.halbuilder;

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.io.CharStreams;
import com.theoryinpractise.halbuilder.xml.XmlReader;

import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import static java.lang.String.format;

public class ResourceFactory {

    private TreeMap<String, String> namespaces = Maps.newTreeMap(Ordering.usingToString());
    private Multimap<String, String> links = ArrayListMultimap.create();
    private Optional<String> baseHref = Optional.absent();

    public ResourceFactory withNamespace(String namespace, String url) {
        if (namespaces.containsKey(namespace)) {
            throw new ResourceException(format("Duplicate namespace '%s' found for resource factory", namespace));
        }
        namespaces.put(namespace, url);
        return this;
    }

    public ResourceFactory withLink(String rel, String url) {
        links.put(rel, url);
        return this;
    }

    public ResourceFactory withBaseHref(String baseHref) {
        this.baseHref = Optional.of(baseHref);
        return this;
    }

    public Resource newHalResource(String href) {
        MutableResource resource = new MutableResource(this, href);

        // Add optional base href
        if (baseHref.isPresent()) {
            resource.withBaseHref(baseHref.get());
        }

        // Add factory standard namespaces
        for (Map.Entry<String, String> entry : namespaces.entrySet()) {
            resource.withNamespace(entry.getKey(), entry.getValue());
        }

        // Add factory standard links
        for (Map.Entry<String, Collection<String>> linkEntry : resource.getLinks().asMap().entrySet()) {
            for (String url : linkEntry.getValue()) {
                resource.withLink(linkEntry.getKey(), url);
            }
        }

        return resource;
    }


    public ReadableResource newHalResource(Reader reader) {
        String halSource = null;
        try {
            halSource = CharStreams.toString(reader);
            if (halSource.startsWith("<")) {
                return new XmlReader().read(halSource);
            }

            throw new IllegalArgumentException("Unknown resource format");
        } catch (IOException e) {
            throw new ResourceException(e);
        }
    }
}
