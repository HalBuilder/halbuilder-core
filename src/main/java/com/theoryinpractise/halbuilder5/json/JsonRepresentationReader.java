package com.theoryinpractise.halbuilder5.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.io.CharStreams;
import com.theoryinpractise.halbuilder5.Link;
import com.theoryinpractise.halbuilder5.Links;
import com.theoryinpractise.halbuilder5.RepresentationException;
import com.theoryinpractise.halbuilder5.ResourceRepresentation;
import com.theoryinpractise.halbuilder5.Support;
import javaslang.collection.List;
import javaslang.collection.TreeMap;
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

public class JsonRepresentationReader {

  private final ObjectMapper mapper;

  public JsonRepresentationReader() {
    this.mapper = new ObjectMapper();
  }

  public ResourceRepresentation<TreeMap<String, Object>> read(Class klazz, Reader reader) {
    try {
      String source = CharStreams.toString(reader);

      JsonNode rootNode = mapper.readValue(new StringReader(source), JsonNode.class);

      return readResource(klazz, rootNode).withContent(ByteString.encodeUtf8(source));

    } catch (Exception e) {
      throw new RepresentationException(e.getMessage(), e);
    }
  }

  public ResourceRepresentation<TreeMap<String, Object>> read(Reader reader) {
    try {
      String source = CharStreams.toString(reader);

      JsonNode rootNode = mapper.readValue(new StringReader(source), JsonNode.class);

      return readResource(TreeMap.class, rootNode).withContent(ByteString.encodeUtf8(source));

    } catch (Exception e) {
      throw new RepresentationException(e.getMessage(), e);
    }
  }

  private <T> ResourceRepresentation<T> readResource(Class<T> klass, JsonNode rootNode) {

    Option<ResourceRepresentation<?>> resource = Option.of(ResourceRepresentation.empty());

    return (ResourceRepresentation<T>)
        resource
            .map(r -> readNamespaces(rootNode, r))
            .map(r -> readLinks(rootNode, r))
            .map(r -> readProperties(rootNode, r))
            .map(r -> readResources(rootNode, r))
            .get();
  }

  private ResourceRepresentation readNamespaces(JsonNode rootNode, ResourceRepresentation resource) {
    ResourceRepresentation newRep = resource;
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

  private ResourceRepresentation readLinks(JsonNode rootNode, ResourceRepresentation resource) {

    List<Link> links = List.empty();

    if (rootNode.has(LINKS)) {
      Iterator<Entry<String, JsonNode>> fields = rootNode.get(LINKS).fields();
      while (fields.hasNext()) {
        Entry<String, JsonNode> keyNode = fields.next();
        if (!CURIES.equals((keyNode.getKey()))) {
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

  private ResourceRepresentation<TreeMap<String, Object>> readProperties(JsonNode rootNode, ResourceRepresentation resource) {
    try {
      TreeMap<String, Object> properties = TreeMap.empty();
      Iterator<String> fieldNames = rootNode.fieldNames();
      while (fieldNames.hasNext()) {
        String fieldName = fieldNames.next();
        if (!Support.RESERVED_JSON_PROPERTIES.contains(fieldName)) {
          JsonNode field = rootNode.get(fieldName);
          if (field.isArray()) {
            List<Object> arrayValues = List.empty();
            for (JsonNode arrayValue : field) {
              arrayValues = arrayValues.append(valueFromNode(arrayValue));
            }
            properties = properties.put(fieldName, arrayValues);
          } else {
            properties = properties.put(fieldName, valueFromNode(field));
          }
        }
      }
      return resource.withValue(properties);
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  private Object valueFromNode(JsonNode field) throws IOException {
    if (field.isNull()) {
      return null;
    } else {
      if (field.isContainerNode()) {
        return TreeMap.ofAll(mapper.readValue(field.toString(), java.util.Map.class));
      } else {
        if (field.isBigDecimal()) {
          return field.decimalValue();
        } else if (field.isBigInteger()) {
          return field.bigIntegerValue();
        } else if (field.isInt()) {
          return field.intValue();
        } else if (field.isBoolean()) {
          return field.booleanValue();
        } else {
          return field.asText();
        }
      }
    }
  }

  private ResourceRepresentation<?> readResources(JsonNode rootNode, ResourceRepresentation resource) {
    if (rootNode.has(EMBEDDED)) {
      ResourceRepresentation newResource = resource;
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
