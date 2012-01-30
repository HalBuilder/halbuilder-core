package com.theoryinpractise.halbuilder.api;

public class ResourceException extends RuntimeException {
    public ResourceException(String s) {
        super(s);
    }

    public ResourceException(Throwable throwable) {
        super(throwable);
    }
}
