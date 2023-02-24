package com.lib.torrent.downloader;

public interface Downloader {

  void start();

  void stop();

  boolean isDownloadComplete();
}
