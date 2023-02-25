package com.lib.torrent.downloader;

public record DownloadedBlock(int pieceIndex, int offset, byte[] data) {

  @Override
  public String toString() {
    return "DownloadedBlock{" +
        "pieceIndex=" + pieceIndex +
        ", offset=" + offset +
        '}';
  }
}
