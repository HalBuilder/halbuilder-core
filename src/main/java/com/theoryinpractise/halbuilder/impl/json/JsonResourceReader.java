package com.theoryinpractise.halbuilder.impl.json;

import com.theoryinpractise.halbuilder.ResourceFactory;
import com.theoryinpractise.halbuilder.impl.api.ResourceReader;
import com.theoryinpractise.halbuilder.impl.resources.MutableResource;
import com.theoryinpractise.halbuilder.spi.ReadableResource;
import com.theoryinpractise.halbuilder.spi.ResourceException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.Reader;
import java.util.Iterator;
import java.util.Map;

import static com.theoryinpractise.halbuilder.impl.api.Support.CURIE;
import static com.theoryinpractise.halbuilder.impl.api.Support.EMBEDDED;
import static com.theoryinpractise.halbuilder.impl.api.Support.HREF;
import static com.theoryinpractise.halbuilder.impl.api.Support.LINKS;
import static com.theoryinpractise.halbuilder.impl.api.Support.NAME;


public class JsonResourceReader implements ResourceReader {
    private ResourceFactory resourceFactory;

    public JsonResourceReader(ResourceFactory resourceFactory) {
        this.resourceFactory = resourceFactory;
    }

    public ReadableResource read(Reader reader) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            JsonNode rootNode = mapper.readValue(reader, JsonNode.class);

            MutableResource resource = readResource(rootNode);

            return resource.asImmutableResource();
        } catch (Exception e) {
            throw new ResourceException(e);
        }

    }

    private MutableResource readResource(JsonNode rootNode) {
        MutableResource resource = new MutableResource(resourceFactory);

        readNamespaces(resource, rootNode);
        readLinks(resource, rootNode);
        readProperties(resource, rootNode);
        readResources(resource, rootNode);
        return resource;
    }

    private void readNamespaces(MutableResource resource, JsonNode rootNode) {
        if (rootNode.has(LINKS)) {
            JsonNode linksNode = rootNode.get(LINKS);
            if (linksNode.has(CURIE)) {
                JsonNode curieNode = linksNode.get(CURIE);

                if (curieNode.isArray()) {
                    Iterator<JsonNode> values = curieNode.getElements();
                    while (values.hasNext()) {
                        JsonNode valueNode = values.next();
                        resource.withNamespace(valueNode.get(NAME).asText(), valueNode.get(HREF).asText());
                    }
                } else {
                    resource.withNamespace(curieNode.get(NAME).asText(), curieNode.get(HREF).asText());
                }
            }
        }
    }

    private void readLinks(MutableResource resource, JsonNode rootNode) {
        if (rootNode.has(LINKS)) {
            Iterator<Map.Entry<String, JsonNode>> fields = rootNode.get(LINKS).getFields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> keyNode = fields.next();
                if (!CURIE.equals((keyNode.getKey()))) {
                    if (keyNode.getValue().isArray()) {
                        Iterator<JsonNode> values = keyNode.getValue().getElements();
                        while (values.hasNext()) {
                            JsonNode valueNode = values.next();
                            resource.withLink(valueNode.get(HREF).asText(), keyNode.getKey());
                        }
                    } else {
                        resource.withLink(keyNode.getValue().get(HREF).asText(), keyNode.getKey());
                    }
                }
            }
        }
    }

    private void readProperties(MutableResource resource, JsonNode rootNode) {

        Iterator<String> fieldNames = rootNode.getFieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            if (!fieldName.startsWith("_")) {
                JsonNode field = rootNode.get(fieldName);
                resource.withProperty(fieldName, field.asText());
            }
        }

    }

    private void readResources(MutableResource resource, JsonNode rootNode) {
        if (rootNode.has(EMBEDDED)) {
            Iterator<Map.Entry<String, JsonNode>> fields = rootNode.get(EMBEDDED).getFields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> keyNode = fields.next();
                if (keyNode.getValue().isArray()) {
                    Iterator<JsonNode> values = keyNode.getValue().getElements();
                    while (values.hasNext()) {
                        JsonNode valueNode = values.next();
                        resource.withSubresource(keyNode.getKey(), readResource(valueNode));
                    }
                } else {
                    resource.withSubresource(keyNode.getKey(), readResource(keyNode.getValue()));
                }

            }
        }
    }
}
