package com.lib.torrent.downloader.exception;

public class ConnectionFailedException extends Exception{

  public ConnectionFailedException(String message) {
    super(message);
  }

  public ConnectionFailedException(String message, Throwable cause) {
    super(message, cause);
  }
}
