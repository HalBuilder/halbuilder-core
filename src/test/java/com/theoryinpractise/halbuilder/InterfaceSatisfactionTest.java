package com.theoryinpractise.halbuilder;

import com.google.common.base.Function;
import org.testng.annotations.DataProvider;
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

    public static interface ISimpleJob {
        Integer jobId();
    }

    @DataProvider
    public Object[][] providerSatisfactionData() {
        return new Object[][] {
                {IPerson.class, true},
                {INamed.class, true},
                {IJob.class, false},
                {ISimpleJob.class, false},
        };
    }

    @Test(dataProvider = "providerSatisfactionData")
    public void testSimpleInterfaceSatisfaction(Class<?> aClass, boolean shouldBeSatisfied) {

        HalResource halResource = HalResource.newHalResource(new InputStreamReader(HalReaderTest.class.getResourceAsStream("example.xml")));
        assertThat(halResource.isSatisfiedBy(aClass)).isEqualTo(shouldBeSatisfied);

    }

    @Test
    public void testClassRendering() {
        HalResource halResource = HalResource.newHalResource(new InputStreamReader(HalReaderTest.class.getResourceAsStream("example.xml")));

        assertThat(halResource.renderClass(INamed.class).get().name()).isEqualTo("Example Resource");
        assertThat(halResource.renderClass(IPerson.class).get().getName()).isEqualTo("Example Resource");
        assertThat(halResource.renderClass(ISimpleJob.class).isPresent()).isFalse();
        assertThat(halResource.renderClass(IJob.class).isPresent()).isFalse();
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
