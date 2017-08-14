package com.theoryinpractise.halbuilder5;

public class RepresentationException extends RuntimeException {
  public RepresentationException(String message) {
    super(message);
  }

  public RepresentationException(Throwable throwable) {
    super(throwable);
  }

  public RepresentationException(String message, Throwable throwable) {
    super(message, throwable);
  }
}
