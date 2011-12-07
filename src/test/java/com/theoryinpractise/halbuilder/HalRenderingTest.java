package com.theoryinpractise.halbuilder;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;

public class HalRenderingTest {

    private String exampleXml;
    private String exampleJson;
    private String exampleWithSubresourceXml;
    private String exampleWithSubresourceJson;

    @BeforeMethod
    public void setup() throws IOException {
        exampleXml = Resources.toString(HalRenderingTest.class.getResource("example.xml"), Charsets.UTF_8)
                .trim().replaceAll("\n", "\r\n");
        exampleJson = Resources.toString(HalRenderingTest.class.getResource("example.json"), Charsets.UTF_8)
                .trim();
        exampleWithSubresourceXml = Resources.toString(HalRenderingTest.class.getResource("exampleWithSubresource.xml"), Charsets.UTF_8)
                .trim().replaceAll("\n", "\r\n");
        exampleWithSubresourceJson = Resources.toString(HalRenderingTest.class.getResource("exampleWithSubresource.json"), Charsets.UTF_8)
                .trim();
    }


    private HalResource newBaseResource(final String href) {
        return HalResource.newHalResource(href)
                .withBaseHref("https://example.com/api/")
                .withNamespace("ns", "/apidocs/accounts")
                .withNamespace("role", "/apidocs/roles")
                .withLink("ns:parent", "/api/customer/1234");
    }


    @Test
    public void testCustomerHal() {

        HalResource party = newBaseResource("customer/123456")
                .withLink("ns:users", "?users")
                .withProperty("id", 123456)
                .withProperty("age", 33)
                .withProperty("name", "Example Resource")
                .withProperty("expired", Boolean.FALSE);

        assertThat(party.renderXml()).isEqualTo(exampleXml);
        assertThat(party.renderJson()).isEqualTo(exampleJson);

    }


    @Test
    public void testHalWithBean() {

        HalResource party = newBaseResource("customer/123456")
                .withLink("ns:users", "?users")
                .withBean(new Customer(123456, "Example Resource", 33));

        assertThat(party.renderXml()).isEqualTo(exampleXml);
        assertThat(party.renderJson()).isEqualTo(exampleJson);

    }

    @Test
    public void testHalWithFields() {

        HalResource party = newBaseResource("customer/123456")
                .withLink("ns:users", "?users")
                .withFields(new OtherCustomer(123456, "Example Resource", 33));

        assertThat(party.renderXml()).isEqualTo(exampleXml);
        assertThat(party.renderJson()).isEqualTo(exampleJson);

    }

    @Test
    public void testHalWithSubResources() {

        HalResource party = newBaseResource("customer/123456")
                .withLink("ns:users", "?users")
                .withSubresource("ns:user role:admin", HalResource
                        .newHalResource("/user/11")
                        .withProperty("id", 11)
                        .withProperty("name", "Example User")
                        .withProperty("expired", false)
                        .withProperty("age", 32));

        assertThat(party.renderXml()).isEqualTo(exampleWithSubresourceXml);
        assertThat(party.renderJson()).isEqualTo(exampleWithSubresourceJson);

    }

    @Test
    public void testHalWithBeanSubResources() {

        HalResource party = newBaseResource("customer/123456")
                .withLink("ns:users", "?users")
                .withBeanBasedSubresource("ns:user role:admin", "/user/11", new Customer(11, "Example User", 32));

        assertThat(party.renderXml()).isEqualTo(exampleWithSubresourceXml);
        assertThat(party.renderJson()).isEqualTo(exampleWithSubresourceJson);

    }

    public static class OtherCustomer {
        public final Integer id;
        public final String name;
        public final Integer age;
        public final Boolean expired = false;

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
    }

}
