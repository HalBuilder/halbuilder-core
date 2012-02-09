package com.theoryinpractise.halbuilder;

import com.theoryinpractise.halbuilder.spi.ReadableResource;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.util.concurrent.ExecutionException;

import static org.fest.assertions.Assertions.assertThat;

public class UrlHalTest {

    @Test
    public void testHalViaUrl() throws MalformedURLException, ExecutionException, InterruptedException {

        ReadableResource resource = new ResourceFactory().openResource(
                "https://raw.github.com/talios/halbuilder/develop/" +
                        "src/test/resources/com/theoryinpractise/halbuilder/example.xml").get();

        assertThat(resource.getProperties().get("name")).isEqualTo("Example Resource");


    }

}
