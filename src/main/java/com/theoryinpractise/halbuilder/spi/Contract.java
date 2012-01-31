package com.theoryinpractise.halbuilder.spi;

public interface Contract {
    boolean isSatisfiedBy(ReadableResource resource);
}
