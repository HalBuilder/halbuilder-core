package com.theoryinpractise.halbuilder.impl.bytecode;

import com.google.common.base.Preconditions;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static com.theoryinpractise.halbuilder.impl.bytecode.InterfaceSupport.derivePropertyNameFromMethod;

/**
 * Java Interface based "renderer", this will write the resource as a Proxy to a Java interface.
 */
public class InterfaceRenderer<T> {

    private Class<T> anInterface;

    public static <I> InterfaceRenderer<I> newInterfaceRenderer(Class<I> anInterface) {
        return new InterfaceRenderer<I>(anInterface);
    }

    private InterfaceRenderer(Class<T> anInterface) {
        Preconditions.checkArgument(anInterface.isInterface(), "Provided class MUST be an interface.");
        this.anInterface = anInterface;
    }

    public T render(final ReadableRepresentation representation) {

        if (representation.isSatisfiedBy(InterfaceContract.newInterfaceContract(anInterface))) {
            T proxy = (T) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{anInterface}, new InvocationHandler() {
                public Object invoke(Object o, Method method, Object[] objects) throws Throwable {

                    String propertyName = derivePropertyNameFromMethod(method);

                    Object propertyValue = representation.getProperties().get(propertyName);

                    Class<?> returnType = method.getReturnType();

                    Object returnValue;

                    if(propertyValue != null) {
                        returnValue = returnType.getConstructor(propertyValue.getClass()).newInstance(propertyValue);
                    } else {
                        // In this case, we have a null property.
                        returnValue = null;
                    }

                    return returnValue;
                }
            });
            return proxy;
        } else {
            throw new RepresentationException("Unable to write representation to " + anInterface.getName());
        }


    }
}
