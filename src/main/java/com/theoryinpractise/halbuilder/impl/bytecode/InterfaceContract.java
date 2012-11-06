package com.theoryinpractise.halbuilder.impl.bytecode;

import com.google.common.base.Preconditions;
import com.theoryinpractise.halbuilder.api.Contract;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;

import java.lang.reflect.Method;

import static com.theoryinpractise.halbuilder.impl.bytecode.InterfaceSupport.derivePropertyNameFromMethod;

/**
 * A Java Interface matching contract
 */
public class InterfaceContract<T> implements Contract {

    private Class<T> anInterface;

    public static <T> InterfaceContract<T> newInterfaceContract(Class<T> anInterface) {
        return new InterfaceContract<T>(anInterface);
    }

    private InterfaceContract(Class<T> anInterface) {
        Preconditions.checkArgument(anInterface.isInterface(), "Contract class MUST be an interface.");
        this.anInterface = anInterface;
    }

    public boolean isSatisfiedBy(ReadableRepresentation representation) {

        for (Method method : anInterface.getDeclaredMethods()) {
            String propertyName = derivePropertyNameFromMethod(method);
            if (!"class".equals(propertyName) && !representation.getProperties().containsKey(propertyName)) {
                return false;
            }
        }

        return true;


    }
}
