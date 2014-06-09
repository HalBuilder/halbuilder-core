package com.theoryinpractise.halbuilder.impl.bytecode;

import com.google.common.base.Preconditions;
import com.theoryinpractise.halbuilder.api.Link;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationException;

import java.lang.reflect.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
        return render(representation.getProperties(), representation.getLinks(), representation.getResourceMap());
    }

    public T render(final Map<String, Object> properties, final List<Link> links, final Map<String, Collection<ReadableRepresentation>> resources) {
        if (InterfaceContract.newInterfaceContract(anInterface).isSatisfiedBy(properties)) {
            T proxy = (T) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] {anInterface}, new InvocationHandler() {
                public Object invoke(Object o, Method method, Object[] objects) throws Throwable {

                    String propertyName = derivePropertyNameFromMethod(method);


                    Object propertyValue = properties.get(propertyName);

                    Class<?> returnType = method.getReturnType();

                    Object returnValue;

                    if (propertyValue != null) {
                        if(propertyValue instanceof Collection) {
                            InterfaceRenderer collectionValueRenderer = new InterfaceRenderer((Class<?>)((((ParameterizedType)method.getGenericReturnType()).getActualTypeArguments()[0])));
                            returnValue = returnType.getConstructor(Collection.class).newInstance(propertyValue);
                            ((Collection) returnValue).clear();
                            for(ReadableRepresentation item : (Collection<ReadableRepresentation>) propertyValue) {
                                ((Collection) returnValue).add(collectionValueRenderer.render(item));
                            }
                        } else {
                            returnValue = returnType.getConstructor(propertyValue.getClass()).newInstance(propertyValue);
                        }
                    } else {
                        // In this case, we have a null property.
                        returnValue = null;
                    }

                    if(method.getName().equals("getLinks")) {
                        return links;
                    }

                    if(method.getName().equals("getEmbedded")) {
                        return resources;
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
