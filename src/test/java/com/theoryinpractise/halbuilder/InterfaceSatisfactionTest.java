package com.theoryinpractise.halbuilder;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.theoryinpractise.halbuilder.impl.bytecode.InterfaceContract;
import com.theoryinpractise.halbuilder.spi.Contract;
import com.theoryinpractise.halbuilder.spi.ReadableResource;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.annotation.Nullable;
import java.io.InputStreamReader;

import static org.fest.assertions.api.Assertions.assertThat;

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
    
    public static interface INullprop {
        String nullprop();
    }

    @DataProvider
    public Object[][] providerSatisfactionData() {
        return new Object[][]{
                {IPerson.class, true},
                {INamed.class, true},
                {IJob.class, false},
                {ISimpleJob.class, false},
        };
    }

    @Test(dataProvider = "providerSatisfactionData")
    public void testSimpleInterfaceSatisfaction(Class<?> aClass, boolean shouldBeSatisfied) {

        ReadableResource resource = resourceFactory.readResource(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("example.xml")));
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
                return resource.getProperties().containsKey("optional") && resource.getProperties().get("optional").get().equals("false");
            }
        };

        Contract contractHasNullProperty = new Contract() {
            public boolean isSatisfiedBy(ReadableResource resource) {
                return resource.getProperties().containsKey("nullprop") && resource.getProperties().get("nullprop").equals(Optional.absent());
            }
        };

        ReadableResource resource = resourceFactory.readResource(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("example.xml")));
        ReadableResource nullPropertyResource = resourceFactory.readResource(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("exampleWithNullProperty.xml")));

        assertThat(resource.isSatisfiedBy(contractHasName)).isEqualTo(true);
        assertThat(resource.isSatisfiedBy(contractHasOptional)).isEqualTo(true);
        assertThat(resource.isSatisfiedBy(contractHasOptionalFalse)).isEqualTo(false);
        assertThat(resource.isSatisfiedBy(contractHasNullProperty)).isEqualTo(false);

        assertThat(nullPropertyResource.isSatisfiedBy(contractHasNullProperty)).isEqualTo(true);
    }

    @Test
    public void testClassRendering() {
        ReadableResource resource = resourceFactory.readResource(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("example.xml")));

        assertThat(resource.renderClass(INamed.class).get().name()).isEqualTo("Example Resource");
        assertThat(resource.renderClass(IPerson.class).get().getName()).isEqualTo("Example Resource");
        assertThat(resource.renderClass(ISimpleJob.class).isPresent()).isFalse();
        assertThat(resource.renderClass(IJob.class).isPresent()).isFalse();
    }
    
    @Test
    public void testNullPropertyClassRendering() {
        ReadableResource resource = resourceFactory.readResource(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("exampleWithNullProperty.xml")));

        assertThat(resource.renderClass(INullprop.class).isPresent()).isTrue();
        assertThat(resource.renderClass(INullprop.class).get().nullprop() == null);
    }

    @Test
    public void testFunctionalInterfaceSatisfaction() {

        ReadableResource resource = resourceFactory.readResource(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("example.xml")));

        String name = resource.ifSatisfiedBy(IPerson.class, new Function<IPerson, String>() {
            public String apply(@Nullable IPerson iPerson) {
                return iPerson.getName();
            }
        }).get();

        assertThat(name).isEqualTo("Example Resource");

    }


}
