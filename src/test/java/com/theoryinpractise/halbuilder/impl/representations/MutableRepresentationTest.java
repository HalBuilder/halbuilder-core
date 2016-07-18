package com.theoryinpractise.halbuilder.impl.representations;

import com.google.common.base.Optional;
import org.testng.annotations.Test;

import static com.theoryinpractise.halbuilder.impl.representations.MutableRepresentation.findPropertyReadMethod;
import static org.fest.assertions.api.Assertions.assertThat;

public class MutableRepresentationTest {

  @Test
  public void testPropertyReader() {
    assertThat(findPropertyReadMethod("getDisplayName")).isEqualTo(Optional.of("displayName"));
    assertThat(findPropertyReadMethod("hasSuperUser")).isEqualTo(Optional.of("superUser"));
    assertThat(findPropertyReadMethod("isSuperUser")).isEqualTo(Optional.of("superUser"));
    assertThat(findPropertyReadMethod("getClass")).isEqualTo(Optional.<String>absent());
    assertThat(findPropertyReadMethod("hashCode")).isEqualTo(Optional.<String>absent());
    assertThat(findPropertyReadMethod("equals")).isEqualTo(Optional.<String>absent());
    assertThat(findPropertyReadMethod("setDisplayName")).isEqualTo(Optional.<String>absent());
  }

}
