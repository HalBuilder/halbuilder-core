package com.theoryinpractise.halbuilder.impl.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationException;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.impl.api.RepresentationReader;
import com.theoryinpractise.halbuilder.impl.representations.MutableRepresentation;

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

public class JsonRepresentationReader implements RepresentationReader {
    private RepresentationFactory representationFactory;

    public JsonRepresentationReader(RepresentationFactory representationFactory) {
        this.representationFactory = representationFactory;
    }

    public ReadableRepresentation read(Reader reader) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            JsonNode rootNode = mapper.readValue(reader, JsonNode.class);

            MutableRepresentation resource = readResource(rootNode);

            return resource.toImmutableResource();
        } catch (Exception e) {
            throw new RepresentationException(e);
        }

    }

    private MutableRepresentation readResource(JsonNode rootNode) {
        MutableRepresentation resource = new MutableRepresentation(representationFactory);

        readNamespaces(resource, rootNode);
        readLinks(resource, rootNode);
        readProperties(resource, rootNode);
        readResources(resource, rootNode);
        return resource;
    }

    private void readNamespaces(MutableRepresentation resource, JsonNode rootNode) {
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

    private void readLinks(MutableRepresentation resource, JsonNode rootNode) {
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

    private void withJsonLink(MutableRepresentation resource, Map.Entry<String, JsonNode> keyNode, JsonNode valueNode) {
        String rel = keyNode.getKey();
        String href = valueNode.get(HREF).asText();
        String name = optionalNodeValueAsText(valueNode, NAME);
        String title = optionalNodeValueAsText(valueNode, TITLE);
        String hreflang = optionalNodeValueAsText(valueNode, HREFLANG);

        resource.withLink(rel, href, name, title, hreflang);
    }

    String optionalNodeValueAsText(JsonNode node, String key) {
        JsonNode value = node.get(key);
        return value != null ? value.asText() : null;
    }

    private void readProperties(MutableRepresentation resource, JsonNode rootNode) {

        Iterator<String> fieldNames = rootNode.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            if (!fieldName.startsWith("_")) {
                JsonNode field = rootNode.get(fieldName);
                resource.withProperty(fieldName, field.isNull() ? null : field.asText());
            }
        }

    }

    private void readResources(MutableRepresentation resource, JsonNode rootNode) {
        if (rootNode.has(EMBEDDED)) {
            Iterator<Map.Entry<String, JsonNode>> fields = rootNode.get(EMBEDDED).fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> keyNode = fields.next();
                if (keyNode.getValue().isArray()) {
                    Iterator<JsonNode> values = keyNode.getValue().elements();
                    while (values.hasNext()) {
                        JsonNode valueNode = values.next();
                        resource.withRepresentation(keyNode.getKey(), readResource(valueNode));
                    }
                } else {
                    resource.withRepresentation(keyNode.getKey(), readResource(keyNode.getValue()));
                }

            }
        }
    }
}
