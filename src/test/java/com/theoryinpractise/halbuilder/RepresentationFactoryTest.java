package com.theoryinpractise.halbuilder;

import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.api.RepresentationReader;
import org.testng.annotations.Test;

import java.io.Reader;
import java.io.StringReader;

import static org.fest.assertions.api.Assertions.assertThat;

public class RepresentationFactoryTest {

    @Test
    public void testWithCustomReader() {
        RepresentationFactory representationFactory = new DefaultRepresentationFactory()
                .withReader(RepresentationFactory.HAL_XML, DummyRepresentationReader.class);
        ReadableRepresentation representation = representationFactory.readRepresentation(new StringReader("<>"));
        assertThat(representation.getProperties().get("name")).isEqualTo("dummy");
    }

    public static class DummyRepresentationReader implements RepresentationReader {
        private final RepresentationFactory representationFactory;

        public DummyRepresentationReader(RepresentationFactory representationFactory) {
            this.representationFactory = representationFactory;
        }

        public ReadableRepresentation read(Reader source) {
            return representationFactory.newRepresentation("/dummy").withProperty("name", "dummy");
        }
    }
}
