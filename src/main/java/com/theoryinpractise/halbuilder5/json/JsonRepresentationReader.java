package com.theoryinpractise.halbuilder5.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Throwables;
import com.google.common.io.CharStreams;
import com.theoryinpractise.halbuilder5.Link;
import com.theoryinpractise.halbuilder5.Links;
import com.theoryinpractise.halbuilder5.RepresentationException;
import com.theoryinpractise.halbuilder5.ResourceRepresentation;
import javaslang.collection.List;
import javaslang.control.Option;
import okio.ByteString;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Map.Entry;

import static com.theoryinpractise.halbuilder5.Support.CURIES;
import static com.theoryinpractise.halbuilder5.Support.EMBEDDED;
import static com.theoryinpractise.halbuilder5.Support.HREF;
import static com.theoryinpractise.halbuilder5.Support.HREFLANG;
import static com.theoryinpractise.halbuilder5.Support.LINKS;
import static com.theoryinpractise.halbuilder5.Support.NAME;
import static com.theoryinpractise.halbuilder5.Support.PROFILE;
import static com.theoryinpractise.halbuilder5.Support.TITLE;
import static okio.ByteString.encodeUtf8;

public class JsonRepresentationReader {

  private final ObjectMapper mapper;

  public JsonRepresentationReader() {
    this.mapper = new ObjectMapper();
  }

  public ResourceRepresentation<ByteString> read(Reader reader) throws IOException {
    return read(ByteString.encodeUtf8(CharStreams.toString(reader)));
  }

  public ResourceRepresentation<ByteString> read(ByteString byteString) {
    try {
      JsonNode rootNode = mapper.readValue(new StringReader(byteString.utf8()), JsonNode.class);
      return readResource(rootNode).withContent(byteString);
    } catch (Exception e) {
      throw new RepresentationException(e.getMessage(), e);
    }
  }

  private ResourceRepresentation<ByteString> readResource(JsonNode rootNode) {

    Option<ResourceRepresentation<Void>> resource = Option.of(ResourceRepresentation.empty());

    return resource
        .map(r -> readNamespaces(rootNode, r))
        .map(r -> readLinks(rootNode, r))
        .map(r -> readProperties(rootNode, r))
        .map(r -> readResources(rootNode, r))
        .get();
  }

  private <T> ResourceRepresentation<T> readNamespaces(JsonNode rootNode, ResourceRepresentation<T> resource) {
    ResourceRepresentation<T> newRep = resource;
    if (rootNode.has(LINKS)) {
      JsonNode linksNode = rootNode.get(LINKS);
      if (linksNode.has(CURIES)) {
        JsonNode curieNode = linksNode.get(CURIES);

        if (curieNode.isArray()) {
          Iterator<JsonNode> values = curieNode.elements();
          while (values.hasNext()) {
            JsonNode valueNode = values.next();
            newRep = newRep.withNamespace(valueNode.get(NAME).asText(), valueNode.get(HREF).asText());
          }
        } else {
          newRep = newRep.withNamespace(curieNode.get(NAME).asText(), curieNode.get(HREF).asText());
        }
      }
    }
    return newRep;
  }

  private <T> ResourceRepresentation<T> readLinks(JsonNode rootNode, ResourceRepresentation<T> resource) {

    List<Link> links = List.empty();

    if (rootNode.has(LINKS)) {
      Iterator<Entry<String, JsonNode>> fields = rootNode.get(LINKS).fields();
      while (fields.hasNext()) {
        Entry<String, JsonNode> keyNode = fields.next();
        if (!CURIES.equals(keyNode.getKey())) {
          if (keyNode.getValue().isArray()) {
            Iterator<JsonNode> values = keyNode.getValue().elements();
            while (values.hasNext()) {
              links = links.append(jsonLink(keyNode.getKey(), values.next()));
            }
          } else {
            links = links.append(jsonLink(keyNode.getKey(), keyNode.getValue()));
          }
        }
      }
    }

    return links.isEmpty() ? resource : resource.withLinks(links);
  }

  private Link jsonLink(String rel, JsonNode node) {
    String href = node.get(HREF).asText();
    String name = optionalNodeValueAsText(node, NAME);
    String title = optionalNodeValueAsText(node, TITLE);
    String hreflang = optionalNodeValueAsText(node, HREFLANG);
    String profile = optionalNodeValueAsText(node, PROFILE);

    return Links.full(rel, href, name, title, hreflang, profile);
  }

  String optionalNodeValueAsText(JsonNode node, String key) {
    JsonNode value = node.get(key);
    return value != null ? value.asText() : "";
  }

  private ResourceRepresentation<ByteString> readProperties(JsonNode rootNode, ResourceRepresentation<?> resource) {
    ObjectNode propertyNode = rootNode.deepCopy();
    propertyNode.remove("_links");
    propertyNode.remove("_embedded");

    try {
      return resource.withValue(encodeUtf8(mapper.writeValueAsString(propertyNode)));
    } catch (JsonProcessingException e) {
      throw Throwables.propagate(e);
    }
  }

  private <T> ResourceRepresentation<T> readResources(JsonNode rootNode, ResourceRepresentation<T> resource) {
    if (rootNode.has(EMBEDDED)) {
      ResourceRepresentation<T> newResource = resource;
      Iterator<Entry<String, JsonNode>> fields = rootNode.get(EMBEDDED).fields();
      while (fields.hasNext()) {
        Entry<String, JsonNode> keyNode = fields.next();
        if (keyNode.getValue().isArray()) {
          Iterator<JsonNode> values = keyNode.getValue().elements();
          while (values.hasNext()) {
            JsonNode valueNode = values.next();
            newResource = newResource.withRepresentation(keyNode.getKey(), readResource(valueNode));
          }
        } else {
          newResource = newResource.withRepresentation(keyNode.getKey(), readResource(keyNode.getValue()));
        }
      }
      return newResource;
    } else {
      return resource;
    }
  }
}
