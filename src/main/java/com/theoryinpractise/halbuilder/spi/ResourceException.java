package com.theoryinpractise.halbuilder.spi;

public class ResourceException extends RuntimeException {
    public ResourceException(String s) {
        super(s);
    }

    public ResourceException(Throwable throwable) {
        super(throwable);
    }
}
