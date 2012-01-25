package com.theoryinpractise.halbuilder.bytecode;

import com.google.common.base.Preconditions;
import com.theoryinpractise.halbuilder.HalContract;
import com.theoryinpractise.halbuilder.HalResource;

import java.lang.reflect.Method;

import static com.theoryinpractise.halbuilder.bytecode.InterfaceSupport.derivePropertyNameFromMethod;

/**
 * A Java Interface matching contract
 */
public class InterfaceContract<T> implements HalContract {

    private Class<T> anInterface;

    public static <T> InterfaceContract<T> createInterfaceContract(Class<T> anInterface) {
        return new InterfaceContract<T>(anInterface);
    }

    private InterfaceContract(Class<T> anInterface) {
        Preconditions.checkArgument(anInterface.isInterface(), "Contract class MUST be an interface.");
        this.anInterface = anInterface;
    }

    public boolean isSatisfiedBy(HalResource resource) {

        for (Method method : anInterface.getDeclaredMethods()) {
            String propertyName = derivePropertyNameFromMethod(method);
            if (!"class".equals(propertyName) && !resource.getProperties().containsKey(propertyName)) {
                return false;
            }
        }

        return true;


    }
}
