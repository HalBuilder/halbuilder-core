package com.theoryinpractise.halbuilder5.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

import static com.theoryinpractise.halbuilder5.Link.HREF;
import static com.theoryinpractise.halbuilder5.Support.CURIES;
import static com.theoryinpractise.halbuilder5.Support.EMBEDDED;
import static com.theoryinpractise.halbuilder5.Support.LINKS;
import static com.theoryinpractise.halbuilder5.Support.TEMPLATED;
import static com.theoryinpractise.halbuilder5.Support.defaultObjectMapper;

public final class JsonRepresentationWriter {

  private ObjectMapper codec;

  private JsonRepresentationWriter(ObjectMapper codec) {
    this.codec = codec;
  }

  public static JsonRepresentationWriter create() {
    return create(defaultObjectMapper());
  }

  public static JsonRepresentationWriter create(Module... modules) {
    return create(defaultObjectMapper(modules));
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

  private ObjectNode renderJson(ResourceRepresentation<?> representation, boolean embedded) {

    ObjectNode objectNode = codec.getNodeFactory().objectNode();

    renderJsonProperties(objectNode, representation);
    renderJsonLinks(objectNode, representation, embedded);
    renderJsonEmbeds(objectNode, representation);

    return objectNode;
  }

  private void renderJsonEmbeds(ObjectNode objectNode, ResourceRepresentation<?> representation) {
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

          List<ResourceRepresentation<?>> values =
              Rels.getComparator(rel)
                  .transform(
                      comp ->
                          comp.isDefined()
                              ? List.ofAll(resourceEntry.getValue()).sorted(comp.get())
                              : List.ofAll(resourceEntry.getValue()));

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

  private void renderJsonProperties(
      ObjectNode objectNode, ResourceRepresentation<?> representation) {
    JsonNode tree = codec.valueToTree(representation.get());
    if (tree != null) {
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
  }

  private void renderJsonLinks(
      ObjectNode objectNode, ResourceRepresentation<?> representation, boolean embedded) {
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

      ObjectNode linksNode = codec.createObjectNode();
      objectNode.set(LINKS, linksNode);

      for (Tuple2<String, List<Link>> linkEntry : links.groupBy(Links::getRel).toList()) {

        Rel rel = representation.getRels().get(linkEntry._1).get();
        boolean coalesce = !isCollection(rel) && (isSingleton(rel) || linkEntry._2.size() == 1);

        if (coalesce) {
          Link link = linkEntry._2.iterator().next();

          ObjectNode linkNode = writeJsonLinkContent(link);
          linksNode.set(linkEntry._1, linkNode);
        } else {
          ArrayNode linkArrayNode = codec.createArrayNode();
          for (Link link : linkEntry._2) {
            linkArrayNode.add(writeJsonLinkContent(link));
          }
          linksNode.set(linkEntry._1, linkArrayNode);
        }
      }
    }
  }

  private ObjectNode writeJsonLinkContent(Link link) {
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
