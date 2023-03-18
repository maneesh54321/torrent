package com.maneesh.network.message;

import com.maneesh.core.Peer;
import java.nio.ByteBuffer;

public class CancelMessage extends NioSocketMessage {

  private static final ByteBuffer cancelMsgBytes = ByteBuffer.allocate(17);

  static {
    cancelMsgBytes.putInt(13);
    cancelMsgBytes.put((byte) 8);
  }

  private final int index;

  private final int begin;

  private final int length;

  private final Peer peer;

  public CancelMessage(int index, int begin, int length, Peer peer) {
    this.index = index;
    this.begin = begin;
    this.length = length;
    this.peer = peer;
  }

  @Override
  public void process() {

  }

  @Override
  protected ByteBuffer convertToBytes() {
    cancelMsgBytes.position(5);
    cancelMsgBytes.putInt(index).putInt(begin).putInt(length).flip();
    return cancelMsgBytes;
  }
}
