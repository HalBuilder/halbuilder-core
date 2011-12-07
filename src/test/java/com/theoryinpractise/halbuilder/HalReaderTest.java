package com.theoryinpractise.halbuilder;

import com.theoryinpractise.halbuilder.reader.XmlHalReader;
import org.testng.annotations.Test;

import java.io.InputStreamReader;

import static org.fest.assertions.Assertions.assertThat;

public class HalReaderTest {

    @Test
    public void testXmlReader() {

        HalResource halResource = new XmlHalReader().read(new InputStreamReader(HalReaderTest.class.getResourceAsStream("example.xml")));

        assertThat(halResource.getHref()).isEqualTo("https://example.com/api/customer/123456");
        assertThat(halResource.getNamespaces()).hasSize(2);
        assertThat(halResource.getLinks().asMap()).hasSize(2);
        assertThat(halResource.getResources().asMap()).hasSize(0);

    }

    @Test
    public void testSubXmlReader() {

        HalResource halResource = new XmlHalReader().read(new InputStreamReader(HalReaderTest.class.getResourceAsStream("exampleWithSubresource.xml")));

        assertThat(halResource.getHref()).isEqualTo("https://example.com/api/customer/123456");
        assertThat(halResource.getNamespaces()).hasSize(2);
        assertThat(halResource.getLinks().asMap()).hasSize(2);
        assertThat(halResource.getResources().asMap()).hasSize(1);

    }

}
