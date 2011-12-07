package com.theoryinpractise.halbuilder.renderer;

import com.theoryinpractise.halbuilder.HalRenderer;
import com.theoryinpractise.halbuilder.HalResource;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.util.DefaultPrettyPrinter;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;

import static com.theoryinpractise.halbuilder.HalResource.resolveRelativeHref;

public class JsonHalRenderer implements HalRenderer {

    public static final String HREF = "_href";
    public static final String CURIES = "_curies";
    public static final String LINKS = "_links";
    public static final String EMBEDDED = "_embedded";

    public void render(HalResource resource, Writer writer) {

        JsonFactory f = new JsonFactory();
        f.enable(JsonGenerator.Feature.QUOTE_FIELD_NAMES);

        try {
            JsonGenerator g = f.createJsonGenerator(writer);
            g.setPrettyPrinter(new DefaultPrettyPrinter());
            g.writeStartObject();
            renderJson(resource.getHref(), g, resource);
            g.writeEndObject();
            g.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void renderJson(String baseHref, JsonGenerator g, HalResource resource) throws IOException {

        g.writeStringField(HREF, resolveRelativeHref(baseHref, resource.getHref()));

        if (!resource.getNamespaces().isEmpty()) {
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
            for (Map.Entry<String, Collection<HalResource>> resourceEntry : resource.getResources().asMap().entrySet()) {
                if (resourceEntry.getValue().size() == 1) {
                    g.writeObjectFieldStart(resourceEntry.getKey());
                    HalResource subResource = resourceEntry.getValue().iterator().next();
                    String subResourceBaseHref = resolveRelativeHref(baseHref, subResource.getHref());
                    renderJson(subResourceBaseHref, g, subResource);
                    g.writeEndObject();
                } else {
                    g.writeArrayFieldStart(resourceEntry.getKey());
                    for (HalResource halResource : resourceEntry.getValue()) {
                        g.writeStartObject();
                        HalResource subResource = resourceEntry.getValue().iterator().next();
                        String subResourceBaseHref = resolveRelativeHref(baseHref, subResource.getHref());
                        renderJson(subResourceBaseHref, g, subResource);
                        g.writeEndObject();
                    }
                }
            }
        }
    }
}
