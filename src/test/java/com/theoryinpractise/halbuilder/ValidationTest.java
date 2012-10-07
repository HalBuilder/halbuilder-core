package com.theoryinpractise.halbuilder;

import com.theoryinpractise.halbuilder.api.Contract;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import org.testng.annotations.Test;

import java.io.InputStreamReader;

import static org.fest.assertions.api.Assertions.assertThat;

public class ValidationTest {


    RepresentationFactory representationFactory = new DefaultRepresentationFactory();

    ReadableRepresentation representation = representationFactory.readRepresentation(
            new InputStreamReader(ValidationTest.class.getResourceAsStream("/example.xml")));

    @Test
    public void testValidation() {

        Contract noWhiteSpaceInName = new Contract() {
            public boolean isSatisfiedBy(ReadableRepresentation resource) {
                return (((String) resource.getValue("name", "")).matches("\\W*"));
            }
        };

        Contract anyCharsInName = new Contract() {
            public boolean isSatisfiedBy(ReadableRepresentation resource) {
                return (((String) resource.getValue("name", "")).matches(".*"));
            }
        };

        assertThat(representation.isSatisfiedBy(noWhiteSpaceInName)).isFalse();
        assertThat(representation.isSatisfiedBy(anyCharsInName)).isTrue();

    }


}
