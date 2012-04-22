package com.theoryinpractise.halbuilder;

import static org.fest.assertions.Assertions.assertThat;

import java.io.InputStreamReader;
import java.io.Reader;

import org.testng.annotations.Test;

import com.theoryinpractise.halbuilder.impl.api.ResourceReader;
import com.theoryinpractise.halbuilder.spi.ReadableResource;

public class ResourceFactoryTest {

    @Test
    public void testWithXmlReader() {
        ResourceFactory resourceFactory = new ResourceFactory()
                .withReader(ResourceFactory.HAL_XML, DummyResourceReader.class);
        ReadableResource resource = resourceFactory.readResource(new InputStreamReader(
                ResourceFactoryTest.class.getResourceAsStream("example.xml")));
        assertThat(resource.getProperties().get("name")).isEqualTo("dummy");
    }

    @Test
    public void testWithJsonReader() {
        ResourceFactory resourceFactory = new ResourceFactory()
                .withReader(ResourceFactory.HAL_JSON, DummyResourceReader.class);
        ReadableResource resource = resourceFactory.readResource(new InputStreamReader(
                ResourceFactoryTest.class.getResourceAsStream("example.json")));
        assertThat(resource.getProperties().get("name")).isEqualTo("dummy");
    }

    public static class DummyResourceReader implements ResourceReader {
        private final ResourceFactory resourceFactory;

        public DummyResourceReader(ResourceFactory resourceFactory) {
            this.resourceFactory = resourceFactory;
        }

        public ReadableResource read(Reader source) {
            return resourceFactory.newResource("/dummy").withProperty("name", "dummy");
        }
    }
}
