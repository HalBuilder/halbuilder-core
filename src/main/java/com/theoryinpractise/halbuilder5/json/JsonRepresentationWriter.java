package com.theoryinpractise.halbuilder5.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.theoryinpractise.halbuilder5.Link;
import com.theoryinpractise.halbuilder5.Links;
import com.theoryinpractise.halbuilder5.Rel;
import com.theoryinpractise.halbuilder5.Rels;
import com.theoryinpractise.halbuilder5.RepresentationException;
import com.theoryinpractise.halbuilder5.ResourceRepresentation;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import okio.Buffer;
import okio.ByteString;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

import static com.theoryinpractise.halbuilder5.Link.HREF;
import static com.theoryinpractise.halbuilder5.Support.CURIES;
import static com.theoryinpractise.halbuilder5.Support.EMBEDDED;
import static com.theoryinpractise.halbuilder5.Support.LINKS;
import static com.theoryinpractise.halbuilder5.Support.TEMPLATED;

public final class JsonRepresentationWriter {

  private ObjectMapper codec;

  private JsonRepresentationWriter(ObjectMapper codec) {
    this.codec = codec;
  }

  public static JsonRepresentationWriter create(Module... modules) {
    return create(getObjectMapper(modules));
  }

  public static JsonRepresentationWriter create(ObjectMapper objectMapper) {
    return new JsonRepresentationWriter(objectMapper);
  }

  public ByteString print(ResourceRepresentation<?> representation) {
    Buffer buffer = new Buffer();
    write(representation, new OutputStreamWriter(buffer.outputStream(), StandardCharsets.UTF_8));
    return buffer.readByteString();
  }

  public void write(ResourceRepresentation representation, Writer writer) {
    try {
      ObjectNode resourceNode = renderJson(representation, false);
      codec.writerWithDefaultPrettyPrinter().writeValue(writer, resourceNode);
    } catch (IOException e) {
      throw new RepresentationException(e);
    }
  }

  private static ObjectMapper getObjectMapper(Module[] modules) {
    JsonFactory f = new JsonFactory();
    f.enable(JsonGenerator.Feature.QUOTE_FIELD_NAMES);

    ObjectMapper objectMapper = new ObjectMapper(f);
    objectMapper.registerModules(modules);
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.configure(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED, false);

    return objectMapper;
  }

  private static final Function<Rel, Boolean> isSingletonF =
      Rels.cases().singleton_(true).otherwise_(false);

  private boolean isSingleton(Rel rel) {
    return isSingletonF.apply(rel);
  }

  private static final Function<Rel, Boolean> isCollectionF =
      Rels.cases().collection_(true).sorted_(true).otherwise_(false);

  private boolean isCollection(Rel rel) {
    return isCollectionF.apply(rel);
  }

  private ObjectNode renderJson(ResourceRepresentation<?> representation, boolean embedded)
      throws IOException {

    ObjectNode objectNode = codec.getNodeFactory().objectNode();

    renderJsonProperties(objectNode, representation);
    renderJsonLinks(objectNode, representation, embedded);
    renderJsonEmbeds(objectNode, representation);

    return objectNode;
  }

  private void renderJsonEmbeds(ObjectNode objectNode, ResourceRepresentation<?> representation)
      throws IOException {
    if (!representation.getResources().isEmpty()) {

      // TODO toJavaMap is kinda nasty
      Map<String, Collection<ResourceRepresentation<?>>> resourceMap =
          representation.getResources().toJavaMap();

      ObjectNode embedsNode = codec.createObjectNode();
      objectNode.set(EMBEDDED, embedsNode);

      for (Map.Entry<String, Collection<ResourceRepresentation<?>>> resourceEntry :
          resourceMap.entrySet()) {

        Rel rel = representation.getRels().get(resourceEntry.getKey()).get();

        boolean coalesce =
            !isCollection(rel) && (isSingleton(rel) || resourceEntry.getValue().size() == 1);

        if (coalesce) {
          ResourceRepresentation<?> subRepresentation = resourceEntry.getValue().iterator().next();
          ObjectNode embeddedNode = renderJson(subRepresentation, true);
          embedsNode.set(resourceEntry.getKey(), embeddedNode);
        } else {

          final Comparator<ResourceRepresentation<?>> repComparator =
              Rels.getComparator(rel).getOrElse(Rel.naturalComparator);

          final List<ResourceRepresentation<?>> values =
              isSingleton(rel)
                  ? List.ofAll(resourceEntry.getValue())
                  : List.ofAll(resourceEntry.getValue()).sorted(repComparator);

          ArrayNode embedArrayNode = codec.createArrayNode();
          embedsNode.set(rel.rel(), embedArrayNode);

          for (ResourceRepresentation<?> subRepresentation : values) {
            ObjectNode embeddedNode = renderJson(subRepresentation, true);
            embedArrayNode.add(embeddedNode);
          }
        }
      }
    }
  }

  private void renderJsonProperties(ObjectNode objectNode, ResourceRepresentation<?> representation)
      throws IOException {
    JsonNode tree = codec.valueToTree(representation.get());
    if (tree.isObject()) {
      Iterator<Map.Entry<String, JsonNode>> fields = tree.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> next = fields.next();
        objectNode.set(next.getKey(), next.getValue());
      }
    } else {
      throw new IllegalStateException("Unable to serialise a non Object Node");
    }
  }

  private void renderJsonLinks(
      ObjectNode objectNode, ResourceRepresentation<?> representation, boolean embedded)
      throws IOException {
    if (!representation.getLinks().isEmpty()
        || (!embedded && !representation.getNamespaces().isEmpty())) {

      List<Link> links = List.empty();

      // Include namespaces as links when not embedded
      if (!embedded) {
        links =
            links.appendAll(
                representation
                    .getNamespaces()
                    .map(ns -> Links.create(CURIES, ns._2, "name", ns._1)));
      }

      // Add representation links
      links = links.appendAll(representation.getLinks());

      // Partition representation links by rel
      Multimap<String, Link> linkMap = Multimaps.index(links, Links::getRel);
      ObjectNode linksNode = codec.createObjectNode();
      objectNode.set(LINKS, linksNode);

      for (Map.Entry<String, Collection<Link>> linkEntry : linkMap.asMap().entrySet()) {

        Rel rel = representation.getRels().get(linkEntry.getKey()).get();
        boolean coalesce =
            !isCollection(rel) && (isSingleton(rel) || linkEntry.getValue().size() == 1);

        if (coalesce) {
          Link link = linkEntry.getValue().iterator().next();

          ObjectNode linkNode = writeJsonLinkContent(link);
          linksNode.set(linkEntry.getKey(), linkNode);
        } else {
          ArrayNode linkArrayNode = codec.createArrayNode();
          for (Link link : linkEntry.getValue()) {
            linkArrayNode.add(writeJsonLinkContent(link));
          }
          linksNode.set(linkEntry.getKey(), linkArrayNode);
        }
      }
    }
  }

  private ObjectNode writeJsonLinkContent(Link link) throws IOException {
    ObjectNode linkNode = codec.createObjectNode();
    linkNode.set(HREF, codec.getNodeFactory().textNode(Links.getHref(link)));

    io.vavr.collection.Map<String, String> properties =
        Links.getProperties(link).getOrElse(HashMap.empty());
    for (Tuple2<String, String> prop : properties) {
      linkNode.set(prop._1, codec.getNodeFactory().textNode(prop._2));
    }
    if (Links.getTemplated(link)) {
      linkNode.set(TEMPLATED, codec.getNodeFactory().booleanNode(true));
    }
    return linkNode;
  }
}