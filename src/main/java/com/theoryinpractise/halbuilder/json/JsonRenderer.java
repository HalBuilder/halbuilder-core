package com.theoryinpractise.halbuilder.json;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.theoryinpractise.halbuilder.Link;
import com.theoryinpractise.halbuilder.ReadableResource;
import com.theoryinpractise.halbuilder.Renderer;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.util.DefaultPrettyPrinter;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class JsonRenderer<T> implements Renderer<T> {

    public static final String HREF = "_href";
    public static final String CURIES = "_curies";
    public static final String LINKS = "_links";
    public static final String EMBEDDED = "_embedded";

    public Optional<T> render(ReadableResource resource, Writer writer) {

        JsonFactory f = new JsonFactory();
        f.enable(JsonGenerator.Feature.QUOTE_FIELD_NAMES);

        try {
            JsonGenerator g = f.createJsonGenerator(writer);
            g.setPrettyPrinter(new DefaultPrettyPrinter());
            g.writeStartObject();
            renderJson(g, resource, false);
            g.writeEndObject();
            g.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Optional.absent();
    }

    private void renderJson(JsonGenerator g, ReadableResource resource, boolean embedded) throws IOException {

        final Link selfLink = resource.getSelfLink();
        final String href = selfLink.getHref();

        // Only include namespaces when not embedded
        if (!embedded && !resource.getNamespaces().isEmpty()) {
            g.writeObjectFieldStart(CURIES);
            for (Map.Entry<String, String> entry : resource.getNamespaces().entrySet()) {
                g.writeStringField(entry.getKey(), entry.getValue());
            }
            g.writeEndObject();
        }

        if (!resource.getCanonicalLinks().isEmpty()) {
            g.writeObjectFieldStart(LINKS);
            List<Link> links = resource.getLinks();

            Multimap<String, Link> linkMap = Multimaps.index(links, new Function<Link, String>() {
                public String apply(@Nullable Link link) {
                    return link.getRel();
                }
            });

            for (Map.Entry<String, Collection<Link>> linkEntry : linkMap.asMap().entrySet()) {


                if (linkEntry.getValue().size() == 1) {
                    g.writeObjectFieldStart(linkEntry.getKey());
                    g.writeStringField(HREF, linkEntry.getValue().iterator().next().getHref());
                    g.writeEndObject();
                } else {
                    g.writeArrayFieldStart(linkEntry.getKey());
                    for (Link link : linkEntry.getValue()) {
                        g.writeStartObject();
                        g.writeStringField(HREF, link.getHref());
                        g.writeEndObject();
                    }
                    g.writeEndArray();
                }
            }
            g.writeEndObject();
        }

        for (Map.Entry<String, Object> entry : resource.getProperties().entrySet()) {
            g.writeObjectField(entry.getKey(), entry.getValue());
        }

        if (!resource.getResources().isEmpty()) {
            g.writeObjectFieldStart(EMBEDDED);
            for (Map.Entry<String, Collection<ReadableResource>> resourceEntry : resource.getResources().asMap().entrySet()) {
                if (resourceEntry.getValue().size() == 1) {
                    g.writeObjectFieldStart(resourceEntry.getKey());
                    ReadableResource subResource = resourceEntry.getValue().iterator().next();
                    renderJson( g, subResource, true);
                    g.writeEndObject();
                } else {
                    g.writeArrayFieldStart(resourceEntry.getKey());
                    for (ReadableResource halResource : resourceEntry.getValue()) {
                        g.writeStartObject();
                        ReadableResource subResource = resourceEntry.getValue().iterator().next();
                        renderJson( g, subResource, true);
                        g.writeEndObject();
                    }
                }
            }
        }
    }
}
