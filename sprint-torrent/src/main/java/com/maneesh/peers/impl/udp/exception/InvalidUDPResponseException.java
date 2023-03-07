package com.maneesh.peers.impl.udp.exception;

public class InvalidUDPResponseException extends Exception {

  public InvalidUDPResponseException(String message) {
    super(message);
  }

  public InvalidUDPResponseException(String message, Throwable cause) {
    super(message, cause);
  }
}
