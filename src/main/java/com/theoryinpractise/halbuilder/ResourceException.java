package com.theoryinpractise.halbuilder;

public class ResourceException extends RuntimeException {
    public ResourceException(String s) {
        super(s);
    }

    public ResourceException(Throwable throwable) {
        super(throwable);
    }
}
