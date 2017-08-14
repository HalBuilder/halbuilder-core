package com.theoryinpractise.halbuilder5;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@JsonSerialize
@Value.Style(
  typeAbstract = {"Abstract*"},
  typeImmutable = "*",
  visibility = Value.Style.ImplementationVisibility.PUBLIC,
  jdkOnly = true,
  defaults = @Value.Immutable(intern = true, builder = false, copy = false)
)
@interface JsonSerializedValue {}
