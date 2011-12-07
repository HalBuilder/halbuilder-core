package com.theoryinpractise.halbuilder;

public class HalResourceException extends RuntimeException {
    public HalResourceException(String s) {
        super(s);
    }

    public HalResourceException(Throwable throwable) {
        super(throwable);
    }
}
