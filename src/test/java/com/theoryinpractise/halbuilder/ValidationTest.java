package com.theoryinpractise.halbuilder;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.theoryinpractise.halbuilder.spi.Contract;
import com.theoryinpractise.halbuilder.spi.ReadableRepresentation;
import org.testng.annotations.Test;

import javax.annotation.Nullable;
import java.io.InputStreamReader;

import static org.fest.assertions.api.Assertions.assertThat;

public class ValidationTest {


    RepresentationFactory representationFactory = new RepresentationFactory();

    ReadableRepresentation representation = representationFactory.readRepresentation(
            new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("example.xml")));


    public static interface Namable {
        String getName();
    }


    @Test
    public void testValidation() {

        Contract noWhiteSpaceInName = new Contract() {
            public boolean isSatisfiedBy(ReadableRepresentation resource) {
                return (((String) resource.get("name").or("")).matches("\\W*"));
            }
        };

        Contract anyCharsInName = new Contract() {
            public boolean isSatisfiedBy(ReadableRepresentation resource) {
                return (((String) resource.get("name").or("")).matches(".*"));
            }
        };

        assertThat(representation.isSatisfiedBy(noWhiteSpaceInName)).isFalse();
        assertThat(representation.isSatisfiedBy(anyCharsInName)).isTrue();

        Optional<Integer> length = representation.ifSatisfiedBy(Namable.class, new Function<Namable, Integer>() {
            public Integer apply(@Nullable Namable input) {
                System.out.println(input.getName());
                return input.getName().length();
            }
        });

        assertThat(length.get()).isEqualTo(16);

    }


}
