package com.theoryinpractise.halbuilder;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.theoryinpractise.halbuilder.spi.Contract;
import com.theoryinpractise.halbuilder.spi.ReadableResource;
import org.testng.annotations.Test;

import javax.annotation.Nullable;
import java.io.InputStreamReader;

import static org.fest.assertions.api.Assertions.assertThat;

public class ValidationTest {


    ResourceFactory resourceFactory = new ResourceFactory();

    ReadableResource resource = resourceFactory.readResource(
            new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("example.xml")));


    public static interface Namable {
        String getName();
    }


    @Test
    public void testValidation() {

        Contract noWhiteSpaceInName = new Contract() {
            public boolean isSatisfiedBy(ReadableResource resource) {
                return (((String) resource.get("name").or("")).matches("\\W*"));
            }
        };

        Contract anyCharsInName = new Contract() {
            public boolean isSatisfiedBy(ReadableResource resource) {
                return (((String) resource.get("name").or("")).matches(".*"));
            }
        };

        assertThat(resource.isSatisfiedBy(noWhiteSpaceInName)).isFalse();
        assertThat(resource.isSatisfiedBy(anyCharsInName)).isTrue();

        Optional<Integer> length = resource.ifSatisfiedBy(Namable.class, new Function<Namable, Integer>() {
            public Integer apply(@Nullable Namable input) {
                System.out.println(input.getName());
                return input.getName().length();
            }
        });

        assertThat(length.get()).isEqualTo(16);

    }


}
