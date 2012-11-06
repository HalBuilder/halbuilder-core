package com.theoryinpractise.halbuilder;

import com.theoryinpractise.halbuilder.api.Link;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationException;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import static org.fest.assertions.api.Assertions.assertThat;

public class ResourceReaderTest {

    private RepresentationFactory representationFactory = new DefaultRepresentationFactory();

    @DataProvider
    public Object[][] provideResources() {
        return new Object[][]{
                {representationFactory.readRepresentation(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("/example.xml")))},
                {representationFactory.readRepresentation(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("/example.json")))},
        };
    }

    @DataProvider
    public Object[][] provideResourcesWithNulls() {
        return new Object[][]{
                {representationFactory.readRepresentation(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("/exampleWithNullProperty.xml")))},
                {representationFactory.readRepresentation(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("/exampleWithNullProperty.json")))},
        };
    }

    @DataProvider
    public Object[][] provideSubResources() {
        return new Object[][]{
                {representationFactory.readRepresentation(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("/exampleWithSubresource.xml")))},
                {representationFactory.readRepresentation(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("/exampleWithSubresource.json")))},
        };
    }

    @DataProvider
    public Object[][] provideResourcesWithouHref() {
        return new Object[][]{
                {representationFactory.readRepresentation(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("/exampleWithoutHref.xml")))},
                {representationFactory.readRepresentation(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("/exampleWithoutHref.json")))},
        };
    }

    @Test(dataProvider = "provideResources")
    public void testReader(ReadableRepresentation representation) {
        assertThat(representation.getResourceLink().getHref()).isEqualTo("https://example.com/api/customer/123456");
        assertThat(representation.getNamespaces()).hasSize(2);
        assertThat(representation.getProperties().get("name")).isEqualTo("Example Resource");
        assertThat(representation.getValue("name")).isEqualTo("Example Resource");
        assertThat(representation.getValue("name")).isEqualTo("Example Resource");
        assertThat(representation.getCanonicalLinks()).hasSize(3);
        assertThat(representation.getResources()).hasSize(0);
        assertThat(representation.getResourcesByRel("role:admin")).hasSize(0);
    }

    @Test(dataProvider = "provideResourcesWithNulls")
    public void testReaderWithNulls(ReadableRepresentation representation) {
        assertThat(representation.getValue("nullprop")).isNull();
        assertThat(representation.getProperties().get("nullprop")).isNull();
    }

    @Test(dataProvider = "provideResources")
    public void testLinkAttributes(ReadableRepresentation representation) {
        Link parent = representation.getLinkByRel("ns:parent");
        assertThat(parent.getHref()).isEqualTo("https://example.com/api/customer/1234");
        assertThat(parent.getRel()).isEqualTo("ns:parent");
        assertThat(parent.getName()).isEqualTo("bob");
        assertThat(parent.getTitle()).isEqualTo("The Parent");
        assertThat(parent.getHreflang()).isEqualTo("en");
    }

    @Test(dataProvider = "provideSubResources")
    public void testSubReader(ReadableRepresentation representation) {
        assertThat(representation.getResourceLink().getHref()).isEqualTo("https://example.com/api/customer/123456");
        assertThat(representation.getNamespaces()).hasSize(2);
        assertThat(representation.getCanonicalLinks()).hasSize(3);
        assertThat(representation.getResources()).hasSize(1);
        assertThat(representation.getResources().iterator().next().getValue().getProperties().get("name")).isEqualTo("Example User");
        assertThat(representation.getResourcesByRel("ns:user")).hasSize(1);
    }

    @Test(dataProvider = "provideResourcesWithouHref")
    public void testResourcesWithoutHref(ReadableRepresentation representation) {
        assertThat(representation.getResourceLink()).isNull();
        assertThat(representation.getNamespaces()).hasSize(0);
        assertThat(representation.getCanonicalLinks()).hasSize(0);
        assertThat(representation.getValue("name")).isEqualTo("Example Resource");
    }

    @Test(expectedExceptions = RepresentationException.class)
    public void testUnknownFormat() {
        representationFactory.readRepresentation(new StringReader("!!!"));
    }

    @Test(expectedExceptions = RepresentationException.class)
    public void testNullReader() {
        representationFactory.readRepresentation((Reader) null);
    }

}
