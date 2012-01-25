package com.theoryinpractise.halbuilder;

import com.google.common.base.Function;
import com.theoryinpractise.halbuilder.bytecode.InterfaceContract;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.annotation.Nullable;
import java.io.InputStreamReader;

import static org.fest.assertions.Assertions.assertThat;

public class InterfaceSatisfactionTest {

    private ResourceFactory resourceFactory = new ResourceFactory();

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

        ReadableResource resource = resourceFactory.newHalResource(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("example.xml")));
        assertThat(resource.isSatisfiedBy(InterfaceContract.newInterfaceContract(aClass))).isEqualTo(shouldBeSatisfied);

    }

    @Test
    public void testAnonymousInnerContractSatisfaction() {

        Contract contractHasName = new Contract() {
            public boolean isSatisfiedBy(ReadableResource resource) {
                return resource.getProperties().containsKey("name");
            }
        };

        Contract contractHasOptional = new Contract() {
            public boolean isSatisfiedBy(ReadableResource resource) {
                return resource.getProperties().containsKey("optional");
            }
        };

        Contract contractHasOptionalFalse = new Contract() {
            public boolean isSatisfiedBy(ReadableResource resource) {
                return resource.getProperties().containsKey("optional") && resource.getProperties().get("optional").equals("false");
            }
        };

        ReadableResource resource = resourceFactory.newHalResource(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("example.xml")));

        assertThat(resource.isSatisfiedBy(contractHasName)).isEqualTo(true);
        assertThat(resource.isSatisfiedBy(contractHasOptional)).isEqualTo(true);
        assertThat(resource.isSatisfiedBy(contractHasOptionalFalse)).isEqualTo(false);

    }

    @Test
    public void testClassRendering() {
        ReadableResource resource = resourceFactory.newHalResource(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("example.xml")));

        assertThat(resource.renderClass(INamed.class).get().name()).isEqualTo("Example Resource");
        assertThat(resource.renderClass(IPerson.class).get().getName()).isEqualTo("Example Resource");
        assertThat(resource.renderClass(ISimpleJob.class).isPresent()).isFalse();
        assertThat(resource.renderClass(IJob.class).isPresent()).isFalse();
    }

    @Test
    public void testFunctionalInterfaceSatisfaction() {

        ReadableResource resource = resourceFactory.newHalResource(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("example.xml")));

        String name = resource.ifSatisfiedBy(IPerson.class, new Function<IPerson, String>() {
            public String apply(@Nullable IPerson iPerson) {
                return iPerson.getName();
            }
        }).get();

        assertThat(name).isEqualTo("Example Resource");

    }


}
