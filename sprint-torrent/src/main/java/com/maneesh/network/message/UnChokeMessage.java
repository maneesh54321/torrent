package com.maneesh.network.message;

import com.maneesh.core.Peer;
import java.nio.ByteBuffer;

public class UnChokeMessage extends NioSocketMessage {

  private final Peer peer;

  public UnChokeMessage(Peer peer) {
    this.peer = peer;
  }

  @Override
  protected ByteBuffer convertToBytes() {
    return null;
  }

  @Override
  public void process() {
    peer.unChoke();
  }
}
