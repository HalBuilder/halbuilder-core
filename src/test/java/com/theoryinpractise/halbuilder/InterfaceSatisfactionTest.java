package com.theoryinpractise.halbuilder;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.theoryinpractise.halbuilder.impl.bytecode.InterfaceContract;
import com.theoryinpractise.halbuilder.spi.Contract;
import com.theoryinpractise.halbuilder.spi.ReadableRepresentation;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.annotation.Nullable;
import java.io.InputStreamReader;

import static org.fest.assertions.api.Assertions.assertThat;

public class InterfaceSatisfactionTest {

    private RepresentationFactory representationFactory = new RepresentationFactory();

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

    @DataProvider
    public Object[][] provideSatisfactionResources() {
        return new Object[][]{
                {representationFactory.readRepresentation(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("example.xml"))), representationFactory.readRepresentation(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("exampleWithNullProperty.xml")))},
                {representationFactory.readRepresentation(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("example.json"))), representationFactory.readRepresentation(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("exampleWithNullProperty.json")))}};
    }

    @Test(dataProvider = "providerSatisfactionData")
    public void testSimpleInterfaceSatisfaction(Class<?> aClass, boolean shouldBeSatisfied) {

        ReadableRepresentation representation = representationFactory.readRepresentation(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("example.xml")));
        assertThat(representation.isSatisfiedBy(InterfaceContract.newInterfaceContract(aClass))).isEqualTo(shouldBeSatisfied);

    }

    @Test(dataProvider = "provideSatisfactionResources")
    public void testAnonymousInnerContractSatisfaction(ReadableRepresentation representation, ReadableRepresentation nullPropertyRepresentation) {

        Contract contractHasName = new Contract() {
            public boolean isSatisfiedBy(ReadableRepresentation resource) {
                return resource.getProperties().containsKey("name");
            }
        };

        Contract contractHasOptional = new Contract() {
            public boolean isSatisfiedBy(ReadableRepresentation resource) {
                return resource.getProperties().containsKey("optional");
            }
        };

        Contract contractHasOptionalFalse = new Contract() {
            public boolean isSatisfiedBy(ReadableRepresentation resource) {
                return resource.getProperties().containsKey("optional") && resource.getProperties().get("optional").get().equals("false");
            }
        };

        Contract contractHasNullProperty = new Contract() {
            public boolean isSatisfiedBy(ReadableRepresentation resource) {
                return resource.getProperties().containsKey("nullprop") && resource.getProperties().get("nullprop").equals(Optional.absent());
            }
        };

        assertThat(representation.isSatisfiedBy(contractHasName)).isEqualTo(true);
        assertThat(representation.isSatisfiedBy(contractHasOptional)).isEqualTo(true);
        assertThat(representation.isSatisfiedBy(contractHasOptionalFalse)).isEqualTo(false);
        assertThat(representation.isSatisfiedBy(contractHasNullProperty)).isEqualTo(false);

        assertThat(nullPropertyRepresentation.isSatisfiedBy(contractHasNullProperty)).isEqualTo(true);
    }

    @Test
    public void testClassRendering() {
        ReadableRepresentation representation = representationFactory.readRepresentation(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("example.xml")));

        assertThat(representation.renderClass(INamed.class).get().name()).isEqualTo("Example Resource");
        assertThat(representation.renderClass(IPerson.class).get().getName()).isEqualTo("Example Resource");
        assertThat(representation.renderClass(ISimpleJob.class).isPresent()).isFalse();
        assertThat(representation.renderClass(IJob.class).isPresent()).isFalse();
    }

    @Test
    public void testNullPropertyClassRendering() {
        ReadableRepresentation representation = representationFactory.readRepresentation(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("exampleWithNullProperty.xml")));

        assertThat(representation.renderClass(INullprop.class).isPresent()).isTrue();
        assertThat(representation.renderClass(INullprop.class).get().nullprop() == null);
    }

    @Test
    public void testFunctionalInterfaceSatisfaction() {

        ReadableRepresentation representation = representationFactory.readRepresentation(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("example.xml")));

        String name = representation.ifSatisfiedBy(IPerson.class, new Function<IPerson, String>() {
            public String apply(@Nullable IPerson iPerson) {
                return iPerson.getName();
            }
        }).get();

        assertThat(name).isEqualTo("Example Resource");

    }


}
