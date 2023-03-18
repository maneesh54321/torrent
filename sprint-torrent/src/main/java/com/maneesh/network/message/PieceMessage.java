package com.maneesh.network.message;

import java.nio.ByteBuffer;

public class PieceMessage extends NioSocketMessage {

  private int pieceIndex;

  private int offset;

  private byte[] data;

  public PieceMessage(int pieceIndex, int offset, byte[] data) {
    this.pieceIndex = pieceIndex;
    this.offset = offset;
    this.data = data;
  }

  @Override
  protected ByteBuffer convertToBytes() {
    return null;
  }

  @Override
  public void process() {

  }
}
