package com.lib.torrent.core.network.exception;

public class BitTorrentProtocolViolationException extends Exception {

  public BitTorrentProtocolViolationException(String s) {
    super(s);
  }

  public BitTorrentProtocolViolationException(String message, Throwable cause) {
    super(message, cause);
  }
}
