package com.theoryinpractise.halbuilder;

import com.theoryinpractise.halbuilder.spi.Link;
import com.theoryinpractise.halbuilder.spi.ReadableRepresentation;
import com.theoryinpractise.halbuilder.spi.RepresentationException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import static org.fest.assertions.api.Assertions.assertThat;

public class ResourceReaderTest {

    private RepresentationFactory representationFactory = new RepresentationFactory();

    @DataProvider
    public Object[][] provideResources() {
        return new Object[][] {
                {representationFactory.readResource(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("example.xml")))},
                {representationFactory.readResource(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("example.json")))},
        };
    }

    @DataProvider
    public Object[][] provideResourcesWithNulls() {
        return new Object[][] {
                {representationFactory.readResource(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("exampleWithNullProperty.xml")))},
                {representationFactory.readResource(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("exampleWithNullProperty.json")))},
        };
    }

    @DataProvider
    public Object[][] provideSubResources() {
        return new Object[][] {
                {representationFactory.readResource(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("exampleWithSubresource.xml")))},
                {representationFactory.readResource(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("exampleWithSubresource.json")))},
        };
    }

    @Test(dataProvider = "provideResources")
    public void testReader(ReadableRepresentation representation) {
        assertThat(representation.getResourceLink().getHref()).isEqualTo("https://example.com/api/customer/123456");
        assertThat(representation.getNamespaces()).hasSize(2);
        assertThat(representation.getProperties().get("name").get()).isEqualTo("Example Resource");
        assertThat(representation.get("name").get()).isEqualTo("Example Resource");
        assertThat(representation.getValue("name")).isEqualTo("Example Resource");
        assertThat(representation.getCanonicalLinks()).hasSize(3);
        assertThat(representation.getResources().values()).hasSize(0);
        assertThat(representation.getResourcesByRel("role:admin")).hasSize(0);
    }

    @Test(dataProvider = "provideResourcesWithNulls")
    public void testReaderWithNulls(ReadableRepresentation representation) {
        assertThat(representation.getValue("nullprop")).isNull();
        assertThat(representation.get("nullprop").isPresent()).isFalse();
        assertThat(representation.getProperties().get("nullprop").isPresent()).isFalse();
    }

    @Test(dataProvider = "provideResources")
    public void testLinkAttributes(ReadableRepresentation representation) {
        Link parent = representation.getLinkByRel("ns:parent").get();
        assertThat(parent.getHref()).isEqualTo("https://example.com/api/customer/1234");
        assertThat(parent.getRel()).isEqualTo("ns:parent");
        assertThat(parent.getName().get()).isEqualTo("bob");
        assertThat(parent.getTitle().get()).isEqualTo("The Parent");
        assertThat(parent.getHreflang().get()).isEqualTo("en");
    }

    @Test(dataProvider = "provideSubResources")
    public void testSubReader(ReadableRepresentation representation) {
        assertThat(representation.getResourceLink().getHref()).isEqualTo("https://example.com/api/customer/123456");
        assertThat(representation.getNamespaces()).hasSize(2);
        assertThat(representation.getCanonicalLinks()).hasSize(3);
        assertThat(representation.getResources().values()).hasSize(1);
        assertThat(representation.getResources().values().iterator().next().getProperties().get("name").get()).isEqualTo("Example User");
        assertThat(representation.getResourcesByRel("ns:user")).hasSize(1);
    }

    @Test(expectedExceptions = RepresentationException.class)
    public void testUnknownFormat() {
        representationFactory.readResource(new StringReader("!!!"));
    }

    @Test(expectedExceptions = RepresentationException.class)
    public void testNullReader() {
        representationFactory.readResource((Reader) null);
    }

}
