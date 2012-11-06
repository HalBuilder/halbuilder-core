package com.theoryinpractise.halbuilder;

import com.theoryinpractise.halbuilder.api.Contract;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationException;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.impl.bytecode.InterfaceContract;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.InputStreamReader;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;

public class InterfaceSatisfactionTest {

    private RepresentationFactory representationFactory = new DefaultRepresentationFactory();

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
                {representationFactory.readRepresentation(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("/example.xml"))), representationFactory.readRepresentation(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("/exampleWithNullProperty.xml")))},
                {representationFactory.readRepresentation(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("/example.json"))), representationFactory.readRepresentation(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("/exampleWithNullProperty.json")))}};
    }

    @Test(dataProvider = "providerSatisfactionData")
    public void testSimpleInterfaceSatisfaction(Class<?> aClass, boolean shouldBeSatisfied) {

        ReadableRepresentation representation = representationFactory.readRepresentation(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("/example.xml")));
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
                return resource.getProperties().containsKey("optional") && resource.getProperties().get("optional").equals("false");
            }
        };

        Contract contractHasNullProperty = new Contract() {
            public boolean isSatisfiedBy(ReadableRepresentation resource) {
                return resource.getProperties().containsKey("nullprop") && resource.getProperties().get("nullprop") == null;
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
        ReadableRepresentation representation = representationFactory.readRepresentation(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("/example.xml")));

        assertThat(representation.toClass(INamed.class).name()).isEqualTo("Example Resource");
        assertThat(representation.toClass(IPerson.class).getName()).isEqualTo("Example Resource");
        try {
            representation.toClass(ISimpleJob.class);
            fail("RepresentationException expected");
        } catch (RepresentationException e) {
            //
        }
        try {
            representation.toClass(IJob.class);
            fail("RepresentationException expected");
        } catch (RepresentationException e) {
            //
        }

    }

    @Test
    public void testNullPropertyClassRendering() {
        ReadableRepresentation representation = representationFactory.readRepresentation(new InputStreamReader(ResourceReaderTest.class.getResourceAsStream("/exampleWithNullProperty.xml")));

        assertThat(representation.toClass(INullprop.class)).isNotNull();
        assertThat(representation.toClass(INullprop.class).nullprop() == null);
    }



}
