package com.theoryinpractise.halbuilder;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import static org.fest.assertions.Assertions.assertThat;

public class ResourceReaderTest {

    private ResourceFactory resourceFactory = new ResourceFactory();

    @DataProvider
    public Object[][] provideResources() {
        return new Object[][]{
                {resourceFactory.newHalResource(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("example.xml")))},
                {resourceFactory.newHalResource(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("example.json")))},
        };
    }

    @DataProvider
    public Object[][] provideSubResources() {
        return new Object[][]{
                {resourceFactory.newHalResource(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("exampleWithSubresource.xml")))},
                {resourceFactory.newHalResource(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("exampleWithSubresource.json")))},
        };
    }

    @Test(dataProvider = "provideResources")
    public void testReader(ReadableResource resource) {
        assertThat(resource.getSelfLink().getHref()).isEqualTo("https://example.com/api/customer/123456");
        assertThat(resource.getNamespaces()).hasSize(2);
        assertThat(resource.getProperties().get("name")).isEqualTo("Example Resource");
        assertThat(resource.getCanonicalLinks()).hasSize(3);
        assertThat(resource.getResources()).hasSize(0);
    }

    @Test(dataProvider = "provideSubResources")
    public void testSubReader(ReadableResource resource) {
        assertThat(resource.getSelfLink().getHref()).isEqualTo("https://example.com/api/customer/123456");
        assertThat(resource.getNamespaces()).hasSize(2);
        assertThat(resource.getCanonicalLinks()).hasSize(3);
        assertThat(resource.getResources()).hasSize(1);
        assertThat(resource.getResources().iterator().next().getProperties().get("name")).isEqualTo("Example User");
    }

    @Test(expectedExceptions = ResourceException.class)
    public void testUnknownFormat() {
        resourceFactory.newHalResource(new StringReader("!!!"));
    }

    @Test(expectedExceptions = ResourceException.class)
    public void testNullReader() {
        resourceFactory.newHalResource((Reader) null);
    }

}
