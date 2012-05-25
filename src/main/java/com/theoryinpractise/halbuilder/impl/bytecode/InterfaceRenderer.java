package com.theoryinpractise.halbuilder.impl.bytecode;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.theoryinpractise.halbuilder.spi.ReadableResource;
import com.theoryinpractise.halbuilder.spi.Renderer;

import java.io.Writer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static com.theoryinpractise.halbuilder.impl.bytecode.InterfaceSupport.derivePropertyNameFromMethod;

/**
 * Java Interface based "renderer", this will render the resource as a Proxy to a Java interface.
 */
public class InterfaceRenderer<T> implements Renderer<T> {

    private Class<T> anInterface;

    public static InterfaceRenderer newInterfaceRenderer(Class<?> anInterface) {
        return new InterfaceRenderer(anInterface);
    }

    private InterfaceRenderer(Class<T> anInterface) {
        Preconditions.checkArgument(anInterface.isInterface(), "Renderable class MUST be an interface.");
        this.anInterface = anInterface;
    }

    public Optional<T> render(final ReadableResource resource, Writer writer) {
        Preconditions.checkArgument(writer == null, "Writer argument should be null for " + InterfaceRenderer.class.getName());

        if (resource.isSatisfiedBy(InterfaceContract.newInterfaceContract(anInterface))) {
            T proxy = (T) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{anInterface}, new InvocationHandler() {
                public Object invoke(Object o, Method method, Object[] objects) throws Throwable {

                    String propertyName = derivePropertyNameFromMethod(method);
                    
                    Optional<Object> propertyOptional = resource.getProperties().get(propertyName);
                    
                    Class<?> returnType = method.getReturnType();
                    
                    Object returnValue;
                    
                    if(propertyOptional.isPresent()) {
                        Object propertyValue = propertyOptional.get();
                        returnValue = returnType.getConstructor(propertyValue.getClass()).newInstance(propertyValue);
                    }
                    else {
                        // In this case, we have a null property.
                        returnValue = null;
                    }
                    
                    return returnValue;
                }
            });
            return Optional.of(proxy);
        } else {
            return Optional.absent();
        }


    }
}
