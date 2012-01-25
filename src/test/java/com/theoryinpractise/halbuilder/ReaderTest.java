package com.theoryinpractise.halbuilder;

import org.testng.annotations.Test;

import java.io.InputStreamReader;

import static org.fest.assertions.Assertions.assertThat;

public class ReaderTest {

    private ResourceFactory resourceFactory = new ResourceFactory();

    @Test
    public void testXmlReader() {

        ReadableResource resource = resourceFactory.newHalResource(new InputStreamReader(ReaderTest.class.getResourceAsStream("example.xml")));

        assertThat(resource.getHref()).isEqualTo("https://example.com/api/customer/123456");
        assertThat(resource.getNamespaces()).hasSize(2);
        assertThat(resource.getLinks().asMap()).hasSize(2);
        assertThat(resource.getResources().asMap()).hasSize(0);

    }

    @Test
    public void testSubXmlReader() {

        ReadableResource resource = resourceFactory.newHalResource(new InputStreamReader(ReaderTest.class.getResourceAsStream("exampleWithSubresource.xml")));

        assertThat(resource.getHref()).isEqualTo("https://example.com/api/customer/123456");
        assertThat(resource.getNamespaces()).hasSize(2);
        assertThat(resource.getLinks().asMap()).hasSize(2);
        assertThat(resource.getResources().asMap()).hasSize(1);

    }

}
