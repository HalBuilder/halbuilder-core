package com.theoryinpractise.halbuilder.impl.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.theoryinpractise.halbuilder.spi.Link;
import com.theoryinpractise.halbuilder.spi.ReadableRepresentation;
import com.theoryinpractise.halbuilder.spi.Renderer;
import com.theoryinpractise.halbuilder.spi.Representation;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.theoryinpractise.halbuilder.impl.api.Support.CURIE;
import static com.theoryinpractise.halbuilder.impl.api.Support.EMBEDDED;
import static com.theoryinpractise.halbuilder.impl.api.Support.HREF;
import static com.theoryinpractise.halbuilder.impl.api.Support.HREFLANG;
import static com.theoryinpractise.halbuilder.impl.api.Support.LINKS;
import static com.theoryinpractise.halbuilder.impl.api.Support.NAME;
import static com.theoryinpractise.halbuilder.impl.api.Support.SELF;
import static com.theoryinpractise.halbuilder.impl.api.Support.TEMPLATED;
import static com.theoryinpractise.halbuilder.impl.api.Support.TITLE;
import static com.theoryinpractise.halbuilder.impl.api.Support.WHITESPACE_SPLITTER;


public class JsonRenderer<T> implements Renderer<T> {

    public Optional<T> render(ReadableRepresentation representation, Writer writer) {

        JsonFactory f = new JsonFactory();
        f.enable(JsonGenerator.Feature.QUOTE_FIELD_NAMES);

        try {
            JsonGenerator g = f.createJsonGenerator(writer);
            g.setPrettyPrinter(new DefaultPrettyPrinter());
            g.writeStartObject();
            renderJson(g, representation, false);
            g.writeEndObject();
            g.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Optional.absent();
    }

    private void renderJson(JsonGenerator g, ReadableRepresentation representation, boolean embedded) throws IOException {

        final Link resourceLink = representation.getResourceLink();
        final String href = resourceLink.getHref();

        if (!representation.getCanonicalLinks().isEmpty() || (!embedded && !representation.getNamespaces().isEmpty())) {
            g.writeObjectFieldStart(LINKS);

            List<Link> links = Lists.newArrayList();

            // Include namespaces as links when not embedded
            if (!embedded) {
                for (Map.Entry<String, String> entry : representation.getNamespaces().entrySet()) {
                    links.add(new Link(null, entry.getValue(), CURIE, Optional.of(entry.getKey()), Optional.<String>absent(), Optional.<String>absent()));
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

        for (Map.Entry<String, Optional<Object>> entry : representation.getProperties().entrySet()) {
            if(entry.getValue().isPresent()) {
                g.writeObjectField(entry.getKey(), entry.getValue().get());
            }
            else {
                g.writeNullField(entry.getKey());
            }
        }

        if (!representation.getResources().isEmpty()) {
            g.writeObjectFieldStart(EMBEDDED);

            Multimap<String, Representation> resourceMap = representation.getResources();

            for (Map.Entry<String, Collection<Representation>> resourceEntry : resourceMap.asMap().entrySet()) {
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
        if (link.getName().isPresent()) {
            g.writeStringField(NAME, link.getName().get());
        }
        if (link.getTitle().isPresent()) {
            g.writeStringField(TITLE, link.getTitle().get());
        }
        if (link.getHreflang().isPresent()) {
            g.writeStringField(HREFLANG, link.getHreflang().get());
        }
        if (link.hasTemplate()) {
            g.writeBooleanField(TEMPLATED, true);
        }
    }
}
