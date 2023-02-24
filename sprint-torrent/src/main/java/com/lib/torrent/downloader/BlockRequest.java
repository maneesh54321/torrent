package com.lib.torrent.downloader;

public class BlockRequest {
  private int pieceIndex;

  private int offset;

  private int length;

  public BlockRequest(int pieceIndex, int offset, int length) {
    this.pieceIndex = pieceIndex;
    this.offset = offset;
    this.length = length;
  }

  public int getPieceIndex() {
    return pieceIndex;
  }

  public int getOffset() {
    return offset;
  }

  public int getLength() {
    return length;
  }

  @Override
  public String toString() {
    return "BlockRequest{" +
        "pieceIndex=" + pieceIndex +
        ", offset=" + offset +
        ", length=" + length +
        '}';
  }
}
