package com.maneesh.network.message;

import java.nio.ByteBuffer;

public class KeepAliveMessage extends NioSocketMessage {

  private static final ByteBuffer buffer = ByteBuffer.allocate(4);

  static {
    buffer.putInt(0);
  }

  @Override
  protected ByteBuffer convertToBytes() {
    buffer.position(0);
    return buffer;
  }

  @Override
  public void process() {

  }
}
