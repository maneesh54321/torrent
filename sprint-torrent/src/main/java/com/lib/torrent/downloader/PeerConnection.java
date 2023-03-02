package com.lib.torrent.downloader;

public interface PeerConnection {
  void start();

  boolean canDownload();

  DownloadedBlock downloadBlock(BlockRequest blockRequest) throws Exception;

  void stop();

  void printState();

  void flushHaveMessages();
}
