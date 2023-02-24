package com.lib.torrent.downloader;

public interface PeerConnection {
  void start();

  boolean isDownloading();

  boolean canDownload();

  void download(int pieceIndex) throws Exception;

  void stop();

  void printState();

  void flushHaveMessages();
}
