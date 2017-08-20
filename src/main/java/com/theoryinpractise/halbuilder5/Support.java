package com.theoryinpractise.halbuilder5;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.Objects;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;

public interface Support {

  Charset DEFAULT_ENCODING = StandardCharsets.UTF_8;

  String LINKS = "_links";
  String EMBEDDED = "_embedded";
  String CURIES = "curies";
  String TEMPLATED = "templated";
  String HAL_JSON = "application/hal+json";
  String HAL_XML = "application/hal+xml";

  static void checkRelType(String rel) {
    Objects.requireNonNull(rel, "Provided rel should not be null.");
    if ("".equals(rel) || rel.contains(" ")) {
      throw new IllegalArgumentException(
          "Provided rel value should be a single rel type, as "
              + "defined by http://tools.ietf.org/html/rfc5988");
    }
  }

  static ObjectMapper defaultObjectMapper(Module... modules) {
    JsonFactory f = new JsonFactory();
    f.enable(JsonGenerator.Feature.QUOTE_FIELD_NAMES);

    ObjectMapper objectMapper = new ObjectMapper(f);
    objectMapper.registerModules(modules);
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.configure(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED, false);

    return objectMapper;
  }
}
