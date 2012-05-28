package com.theoryinpractise.halbuilder.spi;

import com.google.common.base.Optional;
import com.theoryinpractise.halbuilder.ResourceFactory;
import org.testng.annotations.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class LinkTest {
    private ResourceFactory resourceFactory = new ResourceFactory("http://localhost/");

    private Link link = new Link(resourceFactory, "http://example.com/", "rel", Optional.of("name"),
            Optional.of("title"), Optional.of("hreflang"));

    @Test
    public void equalLinksHaveEqualHashCodes() {
        Link otherLink = new Link(resourceFactory, "http://example.com/", "rel", Optional.of("name"),
                Optional.of("title"), Optional.of("hreflang"));
        assertThat(link.hashCode()).isEqualTo(otherLink.hashCode());
    }

    @Test
    public void testHashCodeIsDependentOnHref() {
        Link otherLink = new Link(resourceFactory, "http://example.com/other", "rel", Optional.of("name"),
                Optional.of("title"), Optional.of("hreflang"));
        assertThat(otherLink.hashCode()).isNotEqualTo(link.hashCode());
    }

    @Test
    public void testHashCodeIsDependentOnRel() {
        Link otherLink = new Link(resourceFactory, "http://example.com/", "otherrel", Optional.of("name"),
                Optional.of("title"), Optional.of("hreflang"));
        assertThat(otherLink.hashCode()).isNotEqualTo(link.hashCode());
    }

    @Test
    public void testHashCodeIsDependentOnName() {
        Link otherLink = new Link(resourceFactory, "http://example.com/", "rel", Optional.of("othername"),
                Optional.of("title"), Optional.of("hreflang"));
        assertThat(otherLink.hashCode()).isNotEqualTo(link.hashCode());
    }

    @Test
    public void testHashCodeIsDependentOnTitle() {
        Link otherLink = new Link(resourceFactory, "http://example.com/", "rel", Optional.of("name"),
                Optional.of("othertitle"), Optional.of("hreflang"));
        assertThat(otherLink.hashCode()).isNotEqualTo(link.hashCode());
    }

    @Test
    public void testHashCodeIsDependentOnHreflang() {
        Link otherLink = new Link(resourceFactory, "http://example.com/other", "rel", Optional.of("name"),
                Optional.of("title"), Optional.of("hreflang"));
        assertThat(otherLink.hashCode()).isNotEqualTo(link.hashCode());
    }

    @Test
    public void testEqualsIsDependentOnHref() {
        Link otherLink = new Link(resourceFactory, "http://example.com/other", "rel", Optional.of("name"),
                Optional.of("title"), Optional.of("hreflang"));
        assertThat(otherLink).isNotEqualTo(link);
    }

    @Test
    public void testEqualsIsDependentOnRel() {
        Link otherLink = new Link(resourceFactory, "http://example.com/", "otherrel", Optional.of("name"),
                Optional.of("title"), Optional.of("hreflang"));
        assertThat(otherLink).isNotEqualTo(link);
    }

    @Test
    public void testEqualsIsDependentOnName() {
        Link otherLink = new Link(resourceFactory, "http://example.com/", "rel", Optional.of("othername"),
                Optional.of("title"), Optional.of("hreflang"));
        assertThat(otherLink).isNotEqualTo(link);
    }

    @Test
    public void testEqualsIsDependentOnTitle() {
        Link otherLink = new Link(resourceFactory, "http://example.com/", "rel", Optional.of("name"),
                Optional.of("othertitle"), Optional.of("hreflang"));
        assertThat(otherLink).isNotEqualTo(link);
    }

    @Test
    public void testEqualsIsDependentOnHreflang() {
        Link otherLink = new Link(resourceFactory, "http://example.com/other", "rel", Optional.of("name"),
                Optional.of("title"), Optional.of("hreflang"));
        assertThat(otherLink).isNotEqualTo(link);
    }

    @Test
    public void testToStringRendersHrefRel() {
        String toString = new Link(resourceFactory, "http://example.com/other", "rel").toString();
        assertThat(toString).isEqualTo("<link rel=\"rel\" href=\"http://example.com/other\"/>");
    }

    @Test
    public void testToStringRendersHrefRelNameTitleHreflang() {
        String toString = link.toString();
        assertThat(toString).isEqualTo("<link rel=\"rel\" href=\"http://example.com/\" name=\"name\" title=\"title\" hreflang=\"hreflang\"/>");
    }
    
    @Test
    public void testHasTemplate() {
        Link templateLink = new Link(resourceFactory, "http://example.com/search{?customerId}", "customerSearch");
        assertThat(templateLink.hasTemplate()).isTrue();
    }
    
    @Test
    public void testDoesNotHaveTemplate() {
        Link nonTemplateLink = new Link(resourceFactory, "http://example.com/other", "rel");
        assertThat(nonTemplateLink.hasTemplate()).isFalse();
    }
}
