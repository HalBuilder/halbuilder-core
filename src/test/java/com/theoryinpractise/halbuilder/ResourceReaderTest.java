package com.theoryinpractise.halbuilder;

import com.theoryinpractise.halbuilder.spi.Link;
import com.theoryinpractise.halbuilder.spi.ReadableResource;
import com.theoryinpractise.halbuilder.spi.ResourceException;
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
                {resourceFactory.newResource(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("example.xml")))},
                {resourceFactory.newResource(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("example.json")))},
        };
    }

    @DataProvider
    public Object[][] provideSubResources() {
        return new Object[][]{
                {resourceFactory.newResource(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("exampleWithSubresource.xml")))},
                {resourceFactory.newResource(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("exampleWithSubresource.json")))},
        };
    }

    @Test(dataProvider = "provideResources")
    public void testReader(ReadableResource resource) {
        assertThat(resource.getResourceLink().getHref()).isEqualTo("https://example.com/api/customer/123456");
        assertThat(resource.getNamespaces()).hasSize(2);
        assertThat(resource.getProperties().get("name")).isEqualTo("Example Resource");
        assertThat(resource.getCanonicalLinks()).hasSize(3);
        assertThat(resource.getResources()).hasSize(0);
    }

    @Test(dataProvider = "provideResources")
    public void testLinkAttributes(ReadableResource resource) {
        Link parent = resource.getLinkByRel("ns:parent").get();
        assertThat(parent.getHref()).isEqualTo("https://example.com/api/customer/1234");
        assertThat(parent.getRel()).isEqualTo("ns:parent");
        assertThat(parent.getName().get()).isEqualTo("bob");
        assertThat(parent.getTitle().get()).isEqualTo("The Parent");
        assertThat(parent.getHreflang().get()).isEqualTo("en");
    }

    @Test(dataProvider = "provideSubResources")
    public void testSubReader(ReadableResource resource) {
        assertThat(resource.getResourceLink().getHref()).isEqualTo("https://example.com/api/customer/123456");
        assertThat(resource.getNamespaces()).hasSize(2);
        assertThat(resource.getCanonicalLinks()).hasSize(3);
        assertThat(resource.getResources()).hasSize(1);
        assertThat(resource.getResources().iterator().next().getProperties().get("name")).isEqualTo("Example User");
    }

    @Test(expectedExceptions = ResourceException.class)
    public void testUnknownFormat() {
        resourceFactory.newResource(new StringReader("!!!"));
    }

    @Test(expectedExceptions = ResourceException.class)
    public void testNullReader() {
        resourceFactory.newResource((Reader) null);
    }

}
