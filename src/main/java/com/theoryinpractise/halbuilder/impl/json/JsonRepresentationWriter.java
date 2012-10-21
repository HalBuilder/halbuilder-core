package com.theoryinpractise.halbuilder.impl.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.theoryinpractise.halbuilder.api.*;
import com.theoryinpractise.halbuilder.api.RepresentationWriter;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.theoryinpractise.halbuilder.impl.api.Support.CURIE;
import static com.theoryinpractise.halbuilder.impl.api.Support.EMBEDDED;
import static com.theoryinpractise.halbuilder.impl.api.Support.HREF;
import static com.theoryinpractise.halbuilder.impl.api.Support.HREFLANG;
import static com.theoryinpractise.halbuilder.impl.api.Support.LINKS;
import static com.theoryinpractise.halbuilder.impl.api.Support.NAME;
import static com.theoryinpractise.halbuilder.impl.api.Support.TEMPLATED;
import static com.theoryinpractise.halbuilder.impl.api.Support.TITLE;


public class JsonRepresentationWriter<T> implements RepresentationWriter<T> {

    public void write(ReadableRepresentation representation, Set<URI> flags, Writer writer) {

        JsonFactory f = new JsonFactory();
        f.enable(JsonGenerator.Feature.QUOTE_FIELD_NAMES);

        try {
            JsonGenerator g = f.createJsonGenerator(writer);
            if (flags.contains(RepresentationFactory.PRETTY_PRINT)) {
                g.setPrettyPrinter(new DefaultPrettyPrinter());
            }
            g.writeStartObject();
            renderJson(g, representation, false);
            g.writeEndObject();
            g.close();
        } catch (IOException e) {
            throw new RepresentationException(e);
        }

    }

    private void renderJson(JsonGenerator g, ReadableRepresentation representation, boolean embedded) throws IOException {

        if (!representation.getCanonicalLinks().isEmpty() || (!embedded && !representation.getNamespaces().isEmpty())) {
            g.writeObjectFieldStart(LINKS);

            List<Link> links = Lists.newArrayList();

            // Include namespaces as links when not embedded
            if (!embedded) {
                for (Map.Entry<String, String> entry : representation.getNamespaces().entrySet()) {
                    links.add(new Link(null, entry.getValue(), CURIE, entry.getKey(), null, null));
                }
            }

            // Add representation links
            links.addAll(representation.getLinks());

            // Partition representation links by rel
            Multimap<String, Link> linkMap = Multimaps.index(links, new Function<Link, String>() {
                public String apply(@Nullable Link link) {
                    return link.getRel();
                }
            });

            for (Map.Entry<String, Collection<Link>> linkEntry : linkMap.asMap().entrySet()) {
                if (linkEntry.getValue().size() == 1) {
                    Link link = linkEntry.getValue().iterator().next();
                    g.writeObjectFieldStart(linkEntry.getKey());
                    writeJsonLinkContent(g, link);
                    g.writeEndObject();
                } else {
                    g.writeArrayFieldStart(linkEntry.getKey());
                    for (Link link : linkEntry.getValue()) {
                        g.writeStartObject();
                        writeJsonLinkContent(g, link);
                        g.writeEndObject();
                    }
                    g.writeEndArray();
                }
            }
            g.writeEndObject();
        }

        for (Map.Entry<String, Object> entry : representation.getProperties().entrySet()) {
            if(entry.getValue() != null) {
                g.writeObjectField(entry.getKey(), entry.getValue());
            }
            else {
                g.writeNullField(entry.getKey());
            }
        }

        if (!representation.getResources().isEmpty()) {
            g.writeObjectFieldStart(EMBEDDED);

            Map<String, Collection<ReadableRepresentation>> resourceMap = representation.getResourceMap();

            for (Map.Entry<String, Collection<ReadableRepresentation>> resourceEntry : resourceMap.entrySet()) {
                if (resourceEntry.getValue().size() == 1) {
                    g.writeObjectFieldStart(resourceEntry.getKey());
                    ReadableRepresentation subRepresentation = resourceEntry.getValue().iterator().next();
                    renderJson(g, subRepresentation, true);
                    g.writeEndObject();
                } else {
                    g.writeArrayFieldStart(resourceEntry.getKey());
                    for (ReadableRepresentation subRepresentation : resourceEntry.getValue()) {
                        g.writeStartObject();
                        renderJson(g, subRepresentation, true);
                        g.writeEndObject();
                    }
                    g.writeEndArray();
                }
            }
            g.writeEndObject();
        }
    }

    private void writeJsonLinkContent(JsonGenerator g, Link link) throws IOException {
        g.writeStringField(HREF, link.getHref());
        if (!Strings.isNullOrEmpty(link.getName())) {
            g.writeStringField(NAME, link.getName());
        }
        if (!Strings.isNullOrEmpty(link.getTitle())) {
            g.writeStringField(TITLE, link.getTitle());
        }
        if (!Strings.isNullOrEmpty(link.getHreflang())) {
            g.writeStringField(HREFLANG, link.getHreflang());
        }
        if (link.hasTemplate()) {
            g.writeBooleanField(TEMPLATED, true);
        }
    }
}
