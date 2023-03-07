package com.maneesh.network.exception;

public class BitTorrentProtocolViolationException extends Exception {

  public BitTorrentProtocolViolationException(String s) {
    super(s);
  }

  public BitTorrentProtocolViolationException(String message, Throwable cause) {
    super(message, cause);
  }
}
