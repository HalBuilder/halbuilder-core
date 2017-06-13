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
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import okio.ByteString;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.theoryinpractise.halbuilder5.Link.HREF;
import static com.theoryinpractise.halbuilder5.Link.NAME;
import static com.theoryinpractise.halbuilder5.Support.CURIES;
import static com.theoryinpractise.halbuilder5.Support.EMBEDDED;
import static com.theoryinpractise.halbuilder5.Support.LINKS;
import static okio.ByteString.encodeUtf8;

public class JsonRepresentationReader {

  private final ObjectMapper mapper;

  public static final <T> Function<ByteString, T> readByteStringAs(
      ObjectMapper mapper, Class<T> classType, Supplier<T> defaultValue) {
    return bs -> {
      try {
        return mapper.readValue(bs.utf8(), classType);
      } catch (IOException e) {
        return defaultValue.get();
      }
    };
  }

  public JsonRepresentationReader() {
    this.mapper = new ObjectMapper();
  }

  public <T> ResourceRepresentation<T> read(
      Reader reader, Class<T> classType, Supplier<T> defaultValue) throws IOException {
    return read(encodeUtf8(CharStreams.toString(reader)), classType, defaultValue);
  }

  public ResourceRepresentation<ByteString> read(Reader reader) throws IOException {
    return read(encodeUtf8(CharStreams.toString(reader)));
  }

  public <T> ResourceRepresentation<T> read(
      ByteString byteString, Class<T> classType, Supplier<T> defaultValue) throws IOException {
    return read(byteString).map(readByteStringAs(mapper, classType, defaultValue));
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

    ResourceRepresentation<ByteString> representation =
        readProperties(rootNode, ResourceRepresentation.empty());
    representation = readNamespaces(rootNode, representation);
    representation = readLinks(rootNode, representation);
    representation = readResources(rootNode, representation);

    return representation;
  }

  private <T> ResourceRepresentation<T> readNamespaces(
      JsonNode rootNode, ResourceRepresentation<T> resource) {
    ResourceRepresentation<T> newRep = resource;
    if (rootNode.has(LINKS)) {
      JsonNode linksNode = rootNode.get(LINKS);
      if (linksNode.has(CURIES)) {
        JsonNode curieNode = linksNode.get(CURIES);

        if (curieNode.isArray()) {
          Iterator<JsonNode> values = curieNode.elements();
          while (values.hasNext()) {
            JsonNode valueNode = values.next();
            newRep =
                newRep.withNamespace(valueNode.get(NAME).asText(), valueNode.get(HREF).asText());
          }
        } else {
          newRep = newRep.withNamespace(curieNode.get(NAME).asText(), curieNode.get(HREF).asText());
        }
      }
    }
    return newRep;
  }

  private <T> ResourceRepresentation<T> readLinks(
      JsonNode rootNode, ResourceRepresentation<T> resource) {

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

    Map<String, String> properties = HashMap.empty();
    Iterator<Entry<String, JsonNode>> fields = node.fields();
    while (fields.hasNext()) {
      Entry<String, JsonNode> keyNode = fields.next();
      properties = properties.put(keyNode.getKey(), keyNode.getValue().asText());
    }

    return Links.create(rel, href, properties);
  }

  private ResourceRepresentation<ByteString> readProperties(
      JsonNode rootNode, ResourceRepresentation<?> resource) {
    ObjectNode propertyNode = rootNode.deepCopy();
    propertyNode.remove("_links");
    propertyNode.remove("_embedded");

    try {
      return resource.withValue(encodeUtf8(mapper.writeValueAsString(propertyNode)));
    } catch (JsonProcessingException e) {
      throw Throwables.propagate(e);
    }
  }

  private <T> ResourceRepresentation<T> readResources(
      JsonNode rootNode, ResourceRepresentation<T> resource) {
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
          newResource =
              newResource.withRepresentation(keyNode.getKey(), readResource(keyNode.getValue()));
        }
      }
      return newResource;
    } else {
      return resource;
    }
  }
}
