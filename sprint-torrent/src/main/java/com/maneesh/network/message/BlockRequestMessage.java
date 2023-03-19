package com.maneesh.network.message;

import java.nio.ByteBuffer;
import java.util.Objects;

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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BlockRequestMessage that = (BlockRequestMessage) o;
    return pieceIndex == that.pieceIndex && offset == that.offset && length == that.length;
  }

  @Override
  public int hashCode() {
    return Objects.hash(pieceIndex, offset, length);
  }
}
