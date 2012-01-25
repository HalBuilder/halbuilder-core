package com.theoryinpractise.halbuilder.json;

import com.google.common.base.Optional;
import com.theoryinpractise.halbuilder.Renderer;
import com.theoryinpractise.halbuilder.ReadableResource;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.util.DefaultPrettyPrinter;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;

import static com.theoryinpractise.halbuilder.MutableResource.resolveRelativeHref;

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
            renderJson(resource.getHref(), g, resource, false);
            g.writeEndObject();
            g.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Optional.absent();
    }

    private void renderJson(String baseHref, JsonGenerator g, ReadableResource resource, boolean embedded) throws IOException {

        g.writeStringField(HREF, resolveRelativeHref(baseHref, resource.getHref()));


        // Only include namespaces when not embedded
        if (!embedded && !resource.getNamespaces().isEmpty()) {
            g.writeObjectFieldStart(CURIES);
            for (Map.Entry<String, String> entry : resource.getNamespaces().entrySet()) {
                g.writeStringField(entry.getKey(), resolveRelativeHref(baseHref, entry.getValue()));
            }
            g.writeEndObject();
        }

        if (!resource.getLinks().isEmpty()) {
            g.writeObjectFieldStart(LINKS);
            for (Map.Entry<String, Collection<String>> linkEntry : resource.getLinks().asMap().entrySet()) {
                if (linkEntry.getValue().size() == 1) {
                    g.writeObjectFieldStart(linkEntry.getKey());
                    g.writeStringField(HREF, resolveRelativeHref(baseHref, linkEntry.getValue().iterator().next()));
                    g.writeEndObject();
                } else {
                    g.writeArrayFieldStart(linkEntry.getKey());
                    for (String url : linkEntry.getValue()) {
                        g.writeStartObject();
                        g.writeStringField(HREF, resolveRelativeHref(baseHref, url));
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
                    String subResourceBaseHref = resolveRelativeHref(baseHref, subResource.getHref());
                    renderJson(subResourceBaseHref, g, subResource, true);
                    g.writeEndObject();
                } else {
                    g.writeArrayFieldStart(resourceEntry.getKey());
                    for (ReadableResource halResource : resourceEntry.getValue()) {
                        g.writeStartObject();
                        ReadableResource subResource = resourceEntry.getValue().iterator().next();
                        String subResourceBaseHref = resolveRelativeHref(baseHref, subResource.getHref());
                        renderJson(subResourceBaseHref, g, subResource, true);
                        g.writeEndObject();
                    }
                }
            }
        }
    }
}
