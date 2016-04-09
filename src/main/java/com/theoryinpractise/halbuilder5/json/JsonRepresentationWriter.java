package com.theoryinpractise.halbuilder5.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.theoryinpractise.halbuilder5.Link;
import com.theoryinpractise.halbuilder5.Links;
import com.theoryinpractise.halbuilder5.Rel;
import com.theoryinpractise.halbuilder5.Rels;
import com.theoryinpractise.halbuilder5.RepresentationException;
import com.theoryinpractise.halbuilder5.ResourceRepresentation;
import javaslang.collection.HashSet;
import javaslang.collection.List;
import javaslang.collection.Set;
import javaslang.control.Option;
import okio.Buffer;
import okio.ByteString;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

import static com.theoryinpractise.halbuilder5.Support.CURIES;
import static com.theoryinpractise.halbuilder5.Support.EMBEDDED;
import static com.theoryinpractise.halbuilder5.Support.HREF;
import static com.theoryinpractise.halbuilder5.Support.HREFLANG;
import static com.theoryinpractise.halbuilder5.Support.LINKS;
import static com.theoryinpractise.halbuilder5.Support.NAME;
import static com.theoryinpractise.halbuilder5.Support.PROFILE;
import static com.theoryinpractise.halbuilder5.Support.TEMPLATED;
import static com.theoryinpractise.halbuilder5.Support.TITLE;

public final class JsonRepresentationWriter {

  private ObjectMapper codec;

  private JsonRepresentationWriter(ObjectMapper codec) {
    this.codec = codec;
  }

  public static JsonRepresentationWriter create(Set<URI> flags, Module... modules) {
    return create(getObjectMapper(flags, modules));
  }

  public static JsonRepresentationWriter create(Module... modules) {
    return create(getObjectMapper(HashSet.empty(), modules));
  }

  public static JsonRepresentationWriter create(ObjectMapper objectMapper) {
    return new JsonRepresentationWriter(objectMapper);
  }

  public ByteString print(ResourceRepresentation<?> representation) {
    return print(representation, HashSet.of(ResourceRepresentation.PRETTY_PRINT));
  }

  public ByteString print(ResourceRepresentation<?> representation, Set<URI> flags) {
    Buffer buffer = new Buffer();
    write(representation, flags, new OutputStreamWriter(buffer.outputStream()));
    return buffer.readByteString();
  }

  public void write(ResourceRepresentation representation, Set<URI> flags, Writer writer) {
    try {
      JsonGenerator g = codec.getFactory().createGenerator(writer);
      if (flags.contains(ResourceRepresentation.PRETTY_PRINT)) {
        g.setPrettyPrinter(new DefaultPrettyPrinter());
      }

      g.writeStartObject();
      renderJson(flags, g, representation, false);
      g.writeEndObject();
      g.close();
    } catch (IOException e) {
      throw new RepresentationException(e);
    }
  }

  private static ObjectMapper getObjectMapper(Set<URI> flags, Module[] modules) {
    JsonFactory f = new JsonFactory();
    f.enable(JsonGenerator.Feature.QUOTE_FIELD_NAMES);

    ObjectMapper objectMapper = new ObjectMapper(f);
    objectMapper.registerModules(modules);
    if (flags.contains(ResourceRepresentation.STRIP_NULLS)) {
      objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }
    objectMapper.configure(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED, false);

    return objectMapper;
  }

  private boolean isSingleton(Rel matcher) {
    return matcher.match(Rels.cases((rel) -> true, (rel) -> false, (rel) -> false, (rel, key, comparator) -> false));
  }

  private boolean isCollection(Rel matcher) {
    return matcher.match(Rels.cases((rel) -> false, (rel) -> false, (rel) -> true, (rel, key, comparator) -> false));
  }

  private void renderJson(Set<URI> flags, JsonGenerator g, ResourceRepresentation<?> representation, boolean embedded)
      throws IOException {

    renderJsonProperties(g, representation);
    renderJsonLinks(g, representation, embedded);
    renderJsonEmbeds(flags, g, representation);
  }

  private void renderJsonEmbeds(Set<URI> flags, JsonGenerator g, ResourceRepresentation<?> representation) throws IOException {
    if (!representation.getResources().isEmpty()) {
      g.writeObjectFieldStart(EMBEDDED);

      // TODO toJavaMap is kinda nasty
      Map<String, Collection<ResourceRepresentation<?>>> resourceMap = representation.getResources().toJavaMap();

      for (Map.Entry<String, Collection<ResourceRepresentation<?>>> resourceEntry : resourceMap.entrySet()) {

        Rel rel = representation.getRels().get(resourceEntry.getKey()).get();

        boolean coalesce = !isCollection(rel) && (isSingleton(rel) || resourceEntry.getValue().size() == 1);

        if (coalesce) {
          g.writeObjectFieldStart(resourceEntry.getKey());
          ResourceRepresentation<?> subRepresentation = resourceEntry.getValue().iterator().next();
          renderJson(flags, g, subRepresentation, true);
          g.writeEndObject();
        } else {

          final Comparator<ResourceRepresentation<?>> repComparator = Rels.getComparator(rel).getOrElse(Rel.naturalComparator);

          final List<ResourceRepresentation<?>> values =
              isSingleton(rel)
                  ? List.ofAll(resourceEntry.getValue())
                  : List.ofAll(resourceEntry.getValue()).sorted(repComparator);

          final String collectionRel =
              isSingleton(rel) || flags.contains(ResourceRepresentation.SILENT_SORTING) ? rel.rel() : rel.fullRel();

          g.writeArrayFieldStart(collectionRel);

          for (ResourceRepresentation<?> subRepresentation : values) {
            g.writeStartObject();
            renderJson(flags, g, subRepresentation, true);
            g.writeEndObject();
          }
          g.writeEndArray();
        }
      }
      g.writeEndObject();
    }
  }

  private void renderJsonProperties(JsonGenerator g, ResourceRepresentation<?> representation) throws IOException {
    ObjectMapper objectMapper = (ObjectMapper) g.getCodec();
    JsonNode tree = objectMapper.valueToTree(representation.get());
    if (tree.isObject()) {
      Iterator<Map.Entry<String, JsonNode>> fields = tree.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> next = fields.next();
        g.writeFieldName(next.getKey());
        objectMapper.writeTree(g, next.getValue());
      }
    } else {
      throw new IllegalStateException("Unable to serialise a non Object Node");
    }
  }

  private void renderJsonLinks(JsonGenerator g, ResourceRepresentation<?> representation, boolean embedded) throws IOException {
    if (!representation.getCanonicalLinks().isEmpty() || (!embedded && !representation.getNamespaces().isEmpty())) {
      g.writeObjectFieldStart(LINKS);

      List<Link> links = List.empty();

      // Include namespaces as links when not embedded
      if (!embedded) {
        links = links.appendAll(representation.getNamespaces().map(ns -> Links.named(CURIES, ns._2, ns._1)));
      }

      // Add representation links
      links = links.appendAll(representation.getLinks(false));

      // Partition representation links by rel
      Multimap<String, Link> linkMap = Multimaps.index(links, Links::getRel);

      for (Map.Entry<String, Collection<Link>> linkEntry : linkMap.asMap().entrySet()) {

        Rel rel = representation.getRels().get(linkEntry.getKey()).get();
        boolean coalesce = !isCollection(rel) && (isSingleton(rel) || linkEntry.getValue().size() == 1);

        if (coalesce) {
          Link link = linkEntry.getValue().iterator().next();
          g.writeObjectFieldStart(linkEntry.getKey());
          writeJsonLinkContent(g, link);
          g.writeEndObject();
        } else {
          g.writeArrayFieldStart(linkEntry.getKey());
          for (Link link : linkEntry.getValue()) {
            g.writeStartObject();
            writeJsonLinkContent(g, link);
            g.writeEndObject();
          }
          g.writeEndArray();
        }
      }
      g.writeEndObject();
    }
  }

  private void writeJsonLinkContent(JsonGenerator g, Link link) throws IOException {
    g.writeStringField(HREF, Links.getHref(link));
    writeFieldIfDefined(g, NAME, Links.getName(link));
    writeFieldIfDefined(g, TITLE, Links.getTitle(link));
    writeFieldIfDefined(g, HREFLANG, Links.getHreflang(link));
    writeFieldIfDefined(g, PROFILE, Links.getProfile(link));
    if (link.hasTemplate()) {
      g.writeBooleanField(TEMPLATED, true);
    }
  }

  private void writeFieldIfDefined(JsonGenerator g, String field, Option<String> value) throws IOException {
    if (value.isDefined()) {
      g.writeStringField(field, value.get());
    }
  }
}
