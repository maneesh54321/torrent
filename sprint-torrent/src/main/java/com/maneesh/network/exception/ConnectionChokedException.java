package com.maneesh.network.exception;

import java.io.IOException;

public class ConnectionChokedException extends IOException {

  public ConnectionChokedException(String message) {
    super(message);
  }
}
