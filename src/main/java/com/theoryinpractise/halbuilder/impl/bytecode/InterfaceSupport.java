package com.theoryinpractise.halbuilder.impl.bytecode;

import java.lang.reflect.Method;

public class InterfaceSupport {

    public static String derivePropertyNameFromMethod(Method method) {
        return method.getName().startsWith("get")
                ? method.getName().substring(3, 4).toLowerCase() + method.getName().substring(4)
                : method.getName();
    }

}
