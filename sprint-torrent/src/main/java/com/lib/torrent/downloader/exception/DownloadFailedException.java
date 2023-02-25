package com.lib.torrent.downloader.exception;

public class DownloadFailedException extends Exception {
  public DownloadFailedException(String message){
    super(message);
  }

  public DownloadFailedException(String message, Exception e) {
    super(message, e);
  }
}
