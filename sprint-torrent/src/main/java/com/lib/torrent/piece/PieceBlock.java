package com.lib.torrent.piece;

public class PieceBlock {
  private final int pieceIndex;

  private final int offset;

  private final byte[] data;

  public PieceBlock(int pieceIndex, int offset, byte[] data) {
    this.pieceIndex = pieceIndex;
    this.offset = offset;
    this.data = data;
  }

  public int getPieceIndex() {
    return pieceIndex;
  }

  public int getOffset() {
    return offset;
  }

  public byte[] getData() {
    return data;
  }
}
