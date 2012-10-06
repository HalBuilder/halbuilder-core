package com.theoryinpractise.halbuilder;

import com.damnhandy.uri.template.UriTemplate;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.io.Resources;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.Representation;
import com.theoryinpractise.halbuilder.api.RepresentationException;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.api.Serializable;
import com.theoryinpractise.halbuilder.impl.representations.MutableRepresentation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;

import static org.fest.assertions.api.Assertions.assertThat;

public class RenderingTest {

    private RepresentationFactory representationFactory = new DefaultRepresentationFactory("https://example.com/api/")
            .withNamespace("ns", "/apidocs/accounts")
            .withNamespace("role", "/apidocs/roles")
            .withFlag(RepresentationFactory.PRETTY_PRINT);

    private String exampleXml;
    private String exampleJson;
    private String exampleXmlWithoutHref;
    private String exampleJsonWithoutHref;
    private String exampleWithSubresourceXml;
    private String exampleWithSubresourceJson;
    private String exampleWithSubresourceLinkingToItselfXml;
    private String exampleWithSubresourceLinkingToItselfJson;
    private String exampleWithMultipleSubresourcesXml;
    private String exampleWithMultipleSubresourcesJson;
    private String exampleWithNullPropertyXml;
    private String exampleWithNullPropertyJson;
    private String exampleWithLiteralNullPropertyXml;
    private String exampleWithLiteralNullPropertyJson;
    private String exampleWithMultipleNestedSubresourcesXml;
    private String exampleWithMultipleNestedSubresourcesJson;
    private String exampleWithTemplateXml;
    private String exampleWithTemplateJson;

    @BeforeMethod
    public void setup() throws IOException {
        exampleXmlWithoutHref = Resources.toString(RenderingTest.class.getResource("/exampleWithoutHref.xml"), Charsets.UTF_8)
                .trim().replaceAll("\n", "\r\n");
        exampleJsonWithoutHref = Resources.toString(RenderingTest.class.getResource("/exampleWithoutHref.json"), Charsets.UTF_8)
                .trim();
        exampleXml = Resources.toString(RenderingTest.class.getResource("/example.xml"), Charsets.UTF_8)
                .trim().replaceAll("\n", "\r\n");
        exampleJson = Resources.toString(RenderingTest.class.getResource("/example.json"), Charsets.UTF_8)
                .trim();
        exampleWithSubresourceXml = Resources.toString(RenderingTest.class.getResource("/exampleWithSubresource.xml"), Charsets.UTF_8)
                .trim().replaceAll("\n", "\r\n");
        exampleWithSubresourceJson = Resources.toString(RenderingTest.class.getResource("/exampleWithSubresource.json"), Charsets.UTF_8)
                .trim();
        exampleWithSubresourceLinkingToItselfXml = Resources.toString(RenderingTest.class.getResource("/exampleWithSubresourceLinkingToItself.xml"), Charsets.UTF_8)
                .trim().replaceAll("\n", "\r\n");
        exampleWithSubresourceLinkingToItselfJson = Resources.toString(RenderingTest.class.getResource("/exampleWithSubresourceLinkingToItself.json"), Charsets.UTF_8)
                .trim();
        exampleWithMultipleSubresourcesXml = Resources.toString(RenderingTest.class.getResource("/exampleWithMultipleSubresources.xml"), Charsets.UTF_8)
                .trim().replaceAll("\n", "\r\n");
        exampleWithMultipleSubresourcesJson = Resources.toString(RenderingTest.class.getResource("/exampleWithMultipleSubresources.json"), Charsets.UTF_8)
                .trim();
        exampleWithNullPropertyXml = Resources.toString(RenderingTest.class.getResource("/exampleWithNullProperty.xml"), Charsets.UTF_8)
                .trim().replaceAll("\n", "\r\n");
        exampleWithNullPropertyJson = Resources.toString(RenderingTest.class.getResource("/exampleWithNullProperty.json"), Charsets.UTF_8)
                .trim();
        exampleWithLiteralNullPropertyXml = Resources.toString(RenderingTest.class.getResource("/exampleWithLiteralNullProperty.xml"), Charsets.UTF_8)
                .trim().replaceAll("\n", "\r\n");
        exampleWithLiteralNullPropertyJson = Resources.toString(RenderingTest.class.getResource("/exampleWithLiteralNullProperty.json"), Charsets.UTF_8)
                .trim();
        exampleWithMultipleNestedSubresourcesXml = Resources.toString(RenderingTest.class.getResource("/exampleWithMultipleNestedSubresources.xml"), Charsets.UTF_8)
                .trim().replaceAll("\n", "\r\n");
        exampleWithMultipleNestedSubresourcesJson = Resources.toString(RenderingTest.class.getResource("/exampleWithMultipleNestedSubresources.json"), Charsets.UTF_8)
                .trim();
        exampleWithTemplateXml = Resources.toString(RenderingTest.class.getResource("/exampleWithTemplate.xml"), Charsets.UTF_8)
                .trim().replaceAll("\n", "\r\n");
        exampleWithTemplateJson = Resources.toString(RenderingTest.class.getResource("/exampleWithTemplate.json"), Charsets.UTF_8)
                .trim();
    }


    private Representation newBaseResource(final Representation resource) {
        return resource.withLink("ns:parent", "/api/customer/1234", "bob", "The Parent", "en");
    }

    private Representation newBaseResource(final URI uri) {
        return newBaseResource(representationFactory.newRepresentation(uri));

    }

    private Representation newBaseResource(final String href) {
        return newBaseResource(representationFactory.newRepresentation(href));
    }

    @Test
    public void testFactoryWithLinks() {

        RepresentationFactory representationFactory = new DefaultRepresentationFactory("https://example.com/api/")
                .withLink("/home", "home");

        Representation resource = representationFactory.newRepresentation("/");

        assertThat(resource.getCanonicalLinks()).hasSize(2);
        assertThat(resource.getLinksByRel("home")).hasSize(1);
        assertThat(resource.getLinksByRel("home").iterator().next().toString()).isEqualTo("<link rel=\"home\" href=\"https://example.com/home\"/>");

    }

    @Test(expectedExceptions = RepresentationException.class)
    public void testFactoryWithDuplicateNamespaces() {
        new DefaultRepresentationFactory()
                .withNamespace("home", "https://example.com/api/")
                .withNamespace("home", "https://example.com/api/");
    }


    @Test
    public void testUriBuilderHal() {

        URI path = UriBuilder.fromPath("customer/{id}").buildFromMap(ImmutableMap.of("id", "123456"));

        ReadableRepresentation party = newBaseResource(path)
                .withLink("ns:users", "?users")
                .withProperty("id", 123456)
                .withProperty("age", 33)
                .withProperty("name", "Example Resource")
                .withProperty("optional", Boolean.TRUE)
                .withProperty("expired", Boolean.FALSE);

        assertThat(party.getResourceLink().getHref()).isEqualTo("https://example.com/api/customer/123456");
        assertThat(party.toString(RepresentationFactory.HAL_XML)).isEqualTo(exampleXml);
        assertThat(party.toString(RepresentationFactory.HAL_JSON)).isEqualTo(exampleJson);

    }

    @Test
    public void testResourcesWithoutHref() {

        ReadableRepresentation party = new DefaultRepresentationFactory().newRepresentation()
                .withProperty("name", "Example Resource");

        assertThat(party.getResourceLink()).isNull();
        assertThat(party.toString(RepresentationFactory.HAL_XML, ImmutableSet.of(RepresentationFactory.PRETTY_PRINT))).isEqualTo(exampleXmlWithoutHref);
        assertThat(party.toString(RepresentationFactory.HAL_JSON, ImmutableSet.of(RepresentationFactory.PRETTY_PRINT))).isEqualTo(exampleJsonWithoutHref);

    }

    @Test
    public void testCustomerHal() {

        ReadableRepresentation party = newBaseResource("customer/123456")
                .withLink("ns:users", "?users")
                .withProperty("id", 123456)
                .withProperty("age", 33)
                .withProperty("name", "Example Resource")
                .withProperty("optional", Boolean.TRUE)
                .withProperty("expired", Boolean.FALSE);

        assertThat(party.getResourceLink().getHref()).isEqualTo("https://example.com/api/customer/123456");
        assertThat(party.toString(RepresentationFactory.HAL_XML)).isEqualTo(exampleXml);
        assertThat(party.toString(RepresentationFactory.HAL_JSON)).isEqualTo(exampleJson);

    }

    @Test
    public void testWithSerializable() {

        ReadableRepresentation party = newBaseResource("customer/123456")
                .withLink("ns:users", "?users")
                .withSerializable(new Serializable() {
                    public void serializeResource(Representation resource) {
                        resource.withProperty("id", 123456)
                                .withProperty("age", 33)
                                .withProperty("name", "Example Resource")
                                .withProperty("optional", Boolean.TRUE)
                                .withProperty("expired", Boolean.FALSE);
                    }
                });

        assertThat(party.toString(RepresentationFactory.HAL_XML)).isEqualTo(exampleXml);
        assertThat(party.toString(RepresentationFactory.HAL_JSON)).isEqualTo(exampleJson);

    }


    @Test
    public void testHalWithBean() {

        ReadableRepresentation party = newBaseResource("customer/123456")
                .withLink("ns:users", "?users")
                .withBean(new Customer(123456, "Example Resource", 33));

        assertThat(party.toString(RepresentationFactory.HAL_XML)).isEqualTo(exampleXml);
        assertThat(party.toString(RepresentationFactory.HAL_JSON)).isEqualTo(exampleJson);

    }

    @Test
    public void testHalWithFields() {

        ReadableRepresentation party = newBaseResource("customer/123456")
                .withLink("ns:users", "?users")
                .withFields(new OtherCustomer(123456, "Example Resource", 33));

        assertThat(party.toString(RepresentationFactory.HAL_XML)).isEqualTo(exampleXml);
        assertThat(party.toString(RepresentationFactory.HAL_JSON)).isEqualTo(exampleJson);

    }

    @Test
    public void testHalWithSubResources() {

        ReadableRepresentation party = newBaseResource("customer/123456")
                .withLink("ns:users", "?users")
                .withRepresentation("ns:user", representationFactory
                        .newRepresentation("/user/11")
                        .withProperty("id", 11)
                        .withProperty("name", "Example User")
                        .withProperty("expired", false)
                        .withProperty("age", 32)
                        .withProperty("optional", true));

        assertThat(party.toString(RepresentationFactory.HAL_XML)).isEqualTo(exampleWithSubresourceXml);
        assertThat(party.toString(RepresentationFactory.HAL_JSON)).isEqualTo(exampleWithSubresourceJson);

    }

    @Test
    public void testHalWithSubResourceLinkingToItself() {

        ReadableRepresentation party = newBaseResource("customer/123456")
                .withLink("ns:users", "?users")
                .withRepresentation("ns:user", representationFactory
                        .newRepresentation("/user/11")
                        .withLink("role:admin", "/user/11")
                        .withProperty("id", 11)
                        .withProperty("name", "Example User")
                        .withProperty("expired", false)
                        .withProperty("age", 32)
                        .withProperty("optional", true));

        assertThat(party.toString(RepresentationFactory.HAL_XML)).isEqualTo(exampleWithSubresourceLinkingToItselfXml);
        assertThat(party.toString(RepresentationFactory.HAL_JSON)).isEqualTo(exampleWithSubresourceLinkingToItselfJson);

    }

    @Test
    public void testHalWithBeanSubResource() {

        ReadableRepresentation party = newBaseResource("customer/123456")
                .withLink("ns:users", "?users")
                .withBeanBasedRepresentation("ns:user", "/user/11", new Customer(11, "Example User", 32));

        assertThat(party.toString(RepresentationFactory.HAL_XML)).isEqualTo(exampleWithSubresourceXml);
        assertThat(party.toString(RepresentationFactory.HAL_JSON)).isEqualTo(exampleWithSubresourceJson);

    }

    @Test
    public void testHalWithBeanMultipleSubResources() {

        ReadableRepresentation party = newBaseResource("customer/123456")
                .withLink("ns:users", "?users")
                .withBeanBasedRepresentation("ns:user", "/user/11", new Customer(11, "Example User", 32))
                .withBeanBasedRepresentation("ns:user", "/user/12", new Customer(12, "Example User", 32));

        assertThat(party.toString(RepresentationFactory.HAL_XML)).isEqualTo(exampleWithMultipleSubresourcesXml);
        assertThat(party.toString(RepresentationFactory.HAL_JSON)).isEqualTo(exampleWithMultipleSubresourcesJson);

    }

    @Test
    public void testLinkWithDamnHandyUriTemplate() {

        Phone phone = new Phone(1234, "phone-123");

        String uri = UriTemplate.fromExpression("/customer/phone{?id,number}")
                .set("id", phone.getId())
                .set("number", phone.getNumber())
                .expand();

        ReadableRepresentation representation = newBaseResource("/test").withLink("phone", uri);


        assertThat(representation.getLinkByRel("phone").getHref()).isEqualTo("https://example.com" + uri);

    }

    @Test
    public void testNullPropertyHal() {

        URI path = UriBuilder.fromPath("customer/{id}").buildFromMap(ImmutableMap.of("id", "123456"));

        ReadableRepresentation party = newBaseResource(path)
                .withLink("ns:users", "?users")
                .withProperty("id", 123456)
                .withProperty("age", 33)
                .withProperty("name", "Example Resource")
                .withProperty("optional", Boolean.TRUE)
                .withProperty("expired", Boolean.FALSE)
                .withProperty("nullprop", null);

        assertThat(party.getResourceLink().getHref()).isEqualTo("https://example.com/api/customer/123456");
        assertThat(party.toString(RepresentationFactory.HAL_XML)).isEqualTo(exampleWithNullPropertyXml);
        assertThat(party.toString(RepresentationFactory.HAL_JSON)).isEqualTo(exampleWithNullPropertyJson);
    }

    @Test
    public void testLiteralNullPropertyHal() {
        URI path = UriBuilder.fromPath("customer/{id}").buildFromMap(ImmutableMap.of("id", "123456"));

        ReadableRepresentation party = newBaseResource(path)
                .withLink("ns:users", "?users")
                .withProperty("id", 123456)
                .withProperty("age", 33)
                .withProperty("name", "Example Resource")
                .withProperty("optional", Boolean.TRUE)
                .withProperty("expired", Boolean.FALSE)
                .withProperty("nullval", "null");

        assertThat(party.getResourceLink().getHref()).isEqualTo("https://example.com/api/customer/123456");
        assertThat(party.toString(RepresentationFactory.HAL_XML)).isEqualTo(exampleWithLiteralNullPropertyXml);
        assertThat(party.toString(RepresentationFactory.HAL_JSON)).isEqualTo(exampleWithLiteralNullPropertyJson);
    }

    @Test
    public void testHalWithUriTemplate() {
        ReadableRepresentation party = newBaseResource("customer")
                .withLink("ns:query", "/api/customer/search{?queryParam}");

        assertThat(party.toString(RepresentationFactory.HAL_XML)).isEqualTo(exampleWithTemplateXml);
        assertThat(party.toString(RepresentationFactory.HAL_JSON)).isEqualTo(exampleWithTemplateJson);
    }

    @Test
    public void testHalWithBeanMultipleNestedSubResources() {

        ReadableRepresentation party = newBaseResource("customer/123456")
                .withNamespace("phone", "https://example.com/apidocs/phones")
                .withLink("ns:users", "?users")
                .withBeanBasedRepresentation("ns:user", "/user/11", new Customer(11, "Example User", 32))
                .withBeanBasedRepresentation("ns:user", "/user/12", new Customer(12, "Example User", 32));

        MutableRepresentation mutableRepresentation = (MutableRepresentation) Iterables.getFirst(party.getResources(), null).getValue();
        mutableRepresentation.withBeanBasedRepresentation("phone:cell", "/phone/1", new Phone(1, "555-666-7890"));

        assertThat(party.toString(RepresentationFactory.HAL_XML)).isEqualTo(exampleWithMultipleNestedSubresourcesXml);
        assertThat(party.toString(RepresentationFactory.HAL_JSON)).isEqualTo(exampleWithMultipleNestedSubresourcesJson);
    }

    public static class Phone {
        private final Integer id;

        private final String number;

        public Phone(Integer id, String number) {
            this.id = id;
            this.number = number;
        }

        public Integer getId() {
            return id;
        }

        public String getNumber() {
            return number;
        }
    }

    public static class OtherCustomer {
        public final Integer id;
        public final String name;
        public final Integer age;
        public final Boolean expired = false;
        public final Boolean optional = true;

        public OtherCustomer(Integer id, String name, Integer age) {
            this.id = id;
            this.name = name;
            this.age = age;
        }
    }

    public static class Customer {
        private Integer id;
        private String name;
        private Integer age;
        private Boolean expired = false;
        private Boolean optional = true;

        public Customer(Integer id, String name, Integer age) {
            this.id = id;
            this.name = name;
            this.age = age;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Boolean getExpired() {
            return expired;
        }

        public void setExpired(Boolean expired) {
            this.expired = expired;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public Boolean getOptional() {
            return optional;
        }

        public void setOptional(Boolean optional) {
            this.optional = optional;
        }
    }

}
