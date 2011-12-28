package com.theoryinpractise.halbuilder;

import com.google.common.base.Function;
import org.testng.annotations.Test;

import javax.annotation.Nullable;
import java.io.InputStreamReader;

import static org.fest.assertions.Assertions.assertThat;

public class InterfaceSatisfactionTest {

    public static interface IPerson {
        Integer getAge();
        Boolean getExpired();
        Integer getId();
        String getName();
    }

    public static interface INamed {
        String name();
    }

    public static interface IJob {
        Integer getJobId();
    }

    @Test
    public void testSimpleInterfaceSatisfaction() {

        HalResource halResource = HalResource.newHalResource(new InputStreamReader(HalReaderTest.class.getResourceAsStream("example.xml")));

        assertThat(halResource.isSatisfiedBy(IPerson.class)).isTrue();
        assertThat(halResource.isSatisfiedBy(INamed.class)).isTrue();
        assertThat(halResource.isSatisfiedBy(IJob.class)).isFalse();

    }

    @Test
    public void testFunctionalInterfaceSatisfaction() {

        HalResource halResource = HalResource.newHalResource(new InputStreamReader(HalReaderTest.class.getResourceAsStream("example.xml")));

        String name = halResource.ifSatisfiedBy(IPerson.class, new Function<IPerson, String>() {
            public String apply(@Nullable IPerson iPerson) {
                return iPerson.getName();
            }
        }).get();

        assertThat(name).isEqualTo("Example Resource");

    }


}
