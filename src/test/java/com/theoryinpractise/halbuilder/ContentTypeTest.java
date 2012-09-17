package com.theoryinpractise.halbuilder;

import com.theoryinpractise.halbuilder.impl.ContentType;
import org.fest.assertions.api.Assertions;
import org.testng.annotations.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class ContentTypeTest {

    @Test
    public void testContentTypeCreation() {
        Assertions.assertThat(new ContentType("application/xml").getType()).isEqualTo("application");
        assertThat(new ContentType("application/xml").getSubType()).isEqualTo("xml");
    }

    @Test
    public void testContentTypeMatching() {
        assertThat(new ContentType("application/xml").matches(new ContentType("application/xml"))).isTrue();
        assertThat(new ContentType("application/xml").matches(new ContentType("application/*"))).isTrue();
        assertThat(new ContentType("application/xml").matches(new ContentType("*/*"))).isTrue();
        assertThat(new ContentType("application/xml").matches(new ContentType("*/xml"))).isTrue();
        assertThat(new ContentType("application/xml").matches(new ContentType("*/json"))).isFalse();
        assertThat(new ContentType("*/*").matches(new ContentType("application/xml"))).isFalse();
    }

}
