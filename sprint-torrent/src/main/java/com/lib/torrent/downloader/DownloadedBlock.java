package com.lib.torrent.downloader;

public class DownloadedBlock {
  private final int pieceIndex;

  private final int offset;

  private final byte[] data;

  public DownloadedBlock(int pieceIndex, int offset, byte[] data) {
    this.pieceIndex = pieceIndex;
    this.offset = offset;
    this.data = data;
  }

}
