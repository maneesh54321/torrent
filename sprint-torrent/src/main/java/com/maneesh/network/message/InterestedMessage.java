package com.maneesh.network.message;

import com.maneesh.core.Peer;
import java.nio.ByteBuffer;

public class InterestedMessage extends NioSocketMessage {

  private final Peer peer;

  private static final ByteBuffer interestedMsgBytes = ByteBuffer.allocate(5);

  static {
    interestedMsgBytes.putInt(1);
    interestedMsgBytes.put((byte) 2);
  }

  public InterestedMessage(Peer peer) {
    this.peer = peer;
  }

  @Override
  protected ByteBuffer convertToBytes() {
    interestedMsgBytes.flip();
    return interestedMsgBytes;
  }

  @Override
  public void process() {

  }
}
