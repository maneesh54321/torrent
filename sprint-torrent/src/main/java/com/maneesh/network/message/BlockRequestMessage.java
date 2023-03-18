package com.maneesh.network.message;

import java.nio.ByteBuffer;

public class BlockRequestMessage extends NioSocketMessage {

  private final int pieceIndex;

  private final int offset;

  private final int length;

  public BlockRequestMessage(int pieceIndex, int offset, int length) {
    this.pieceIndex = pieceIndex;
    this.offset = offset;
    this.length = length;
  }

  @Override
  protected ByteBuffer convertToBytes() {
    ByteBuffer message = ByteBuffer.allocate(17);
    message.putInt(13);
    message.put((byte) 6);
    message.putInt(pieceIndex);
    message.putInt(offset);
    message.putInt(length);
    message.flip();
    return message;
  }

  @Override
  public void process() {
  }

  @Override
  public String toString() {
    return "BlockRequestMessage{" +
        "pieceIndex=" + pieceIndex +
        ", offset=" + offset +
        ", length=" + length +
        '}';
  }
}
