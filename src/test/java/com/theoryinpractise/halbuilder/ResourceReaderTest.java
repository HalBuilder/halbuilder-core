package com.theoryinpractise.halbuilder;

import com.theoryinpractise.halbuilder.spi.Link;
import com.theoryinpractise.halbuilder.spi.ReadableResource;
import com.theoryinpractise.halbuilder.spi.ResourceException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import static org.fest.assertions.api.Assertions.assertThat;

public class ResourceReaderTest {

    private ResourceFactory resourceFactory = new ResourceFactory();

    @DataProvider
    public Object[][] provideResources() {
        return new Object[][] {
                {resourceFactory.readResource(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("example.xml")))},
                {resourceFactory.readResource(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("example.json")))},
        };
    }
    
    @DataProvider
    public Object[][] provideResourcesWithNulls() {
        return new Object[][] {
                {resourceFactory.readResource(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("exampleWithNullProperty.xml")))},
                {resourceFactory.readResource(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("exampleWithNullProperty.json")))},
        };
    }

    @DataProvider
    public Object[][] provideSubResources() {
        return new Object[][] {
                {resourceFactory.readResource(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("exampleWithSubresource.xml")))},
                {resourceFactory.readResource(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("exampleWithSubresource.json")))},
        };
    }

    @Test(dataProvider = "provideResources")
    public void testReader(ReadableResource resource) {
        assertThat(resource.getResourceLink().getHref()).isEqualTo("https://example.com/api/customer/123456");
        assertThat(resource.getNamespaces()).hasSize(2);
        assertThat(resource.getProperties().get("name").get()).isEqualTo("Example Resource");
        assertThat(resource.get("name").get()).isEqualTo("Example Resource");
        assertThat(resource.getValue("name")).isEqualTo("Example Resource");
        assertThat(resource.getCanonicalLinks()).hasSize(3);
        assertThat(resource.getResources()).hasSize(0);
        assertThat(resource.getResourcesByRel("role:admin")).hasSize(0);
    }
    
    @Test(dataProvider = "provideResourcesWithNulls")
    public void testReaderWithNulls(ReadableResource resource) {
        assertThat(resource.getValue("nullprop")).isNull();
        assertThat(resource.get("nullprop").isPresent()).isFalse();
        assertThat(resource.getProperties().get("nullprop").isPresent()).isFalse();
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
        assertThat(resource.getResources().iterator().next().getProperties().get("name").get()).isEqualTo("Example User");
        assertThat(resource.getResourcesByRel("role:admin")).hasSize(1);
    }

    @Test(expectedExceptions = ResourceException.class)
    public void testUnknownFormat() {
        resourceFactory.readResource(new StringReader("!!!"));
    }

    @Test(expectedExceptions = ResourceException.class)
    public void testNullReader() {
        resourceFactory.readResource((Reader) null);
    }

}
