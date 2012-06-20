package com.theoryinpractise.halbuilder.impl.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.theoryinpractise.halbuilder.ResourceFactory;
import com.theoryinpractise.halbuilder.impl.api.ResourceReader;
import com.theoryinpractise.halbuilder.impl.resources.MutableResource;
import com.theoryinpractise.halbuilder.spi.ReadableResource;
import com.theoryinpractise.halbuilder.spi.ResourceException;

import java.io.Reader;
import java.util.Iterator;
import java.util.Map;

import static com.theoryinpractise.halbuilder.impl.api.Support.CURIE;
import static com.theoryinpractise.halbuilder.impl.api.Support.EMBEDDED;
import static com.theoryinpractise.halbuilder.impl.api.Support.HREF;
import static com.theoryinpractise.halbuilder.impl.api.Support.HREFLANG;
import static com.theoryinpractise.halbuilder.impl.api.Support.LINKS;
import static com.theoryinpractise.halbuilder.impl.api.Support.NAME;
import static com.theoryinpractise.halbuilder.impl.api.Support.TITLE;

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

            return resource.toImmutableResource();
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
                    Iterator<JsonNode> values = curieNode.elements();
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
            Iterator<Map.Entry<String, JsonNode>> fields = rootNode.get(LINKS).fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> keyNode = fields.next();
                if (!CURIE.equals((keyNode.getKey()))) {
                    if (keyNode.getValue().isArray()) {
                        Iterator<JsonNode> values = keyNode.getValue().elements();
                        while (values.hasNext()) {
                            JsonNode valueNode = values.next();
                            withJsonLink(resource, keyNode, valueNode);
                        }
                    } else {
                        withJsonLink(resource, keyNode, keyNode.getValue());
                    }
                }
            }
        }
    }

    private void withJsonLink(MutableResource resource, Map.Entry<String, JsonNode> keyNode, JsonNode valueNode) {
        String rel = keyNode.getKey();
        String href = valueNode.get(HREF).asText();
        Optional<String> name = optionalNodeValueAsText(valueNode, NAME);
        Optional<String> title = optionalNodeValueAsText(valueNode, TITLE);
        Optional<String> hreflang = optionalNodeValueAsText(valueNode, HREFLANG);
        Optional<Predicate<ReadableResource>> predicate = Optional.<Predicate<ReadableResource>>absent();

        resource.withLink(href, rel, predicate, name, title, hreflang );
    }

    Optional<String> optionalNodeValueAsText(JsonNode node, String key) {
        JsonNode value = node.get(key);
        return value != null ? Optional.of(value.asText()) : Optional.<String>absent();
    }

    private void readProperties(MutableResource resource, JsonNode rootNode) {

        Iterator<String> fieldNames = rootNode.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            if (!fieldName.startsWith("_")) {
                JsonNode field = rootNode.get(fieldName);
                resource.withProperty(fieldName, field.isNull() ? null : field.asText());
            }
        }

    }

    private void readResources(MutableResource resource, JsonNode rootNode) {
        if (rootNode.has(EMBEDDED)) {
            Iterator<Map.Entry<String, JsonNode>> fields = rootNode.get(EMBEDDED).fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> keyNode = fields.next();
                if (keyNode.getValue().isArray()) {
                    Iterator<JsonNode> values = keyNode.getValue().elements();
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
