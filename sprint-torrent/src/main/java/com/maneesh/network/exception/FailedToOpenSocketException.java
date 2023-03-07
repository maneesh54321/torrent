package com.maneesh.network.exception;

public class FailedToOpenSocketException extends Exception {

  public FailedToOpenSocketException(String message) {
    super(message);
  }

  public FailedToOpenSocketException(String message, Throwable cause) {
    super(message, cause);
  }
}
