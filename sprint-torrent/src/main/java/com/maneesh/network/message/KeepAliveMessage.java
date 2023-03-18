package com.maneesh.network.message;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class KeepAliveMessage extends NioSocketMessage {

  @Override
  protected ByteBuffer convertToBytes() {
    return null;
  }

  @Override
  public void process() {

  }
}
