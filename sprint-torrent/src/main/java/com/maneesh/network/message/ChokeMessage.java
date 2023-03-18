package com.maneesh.network.message;

import com.maneesh.core.Peer;
import java.nio.ByteBuffer;

public class ChokeMessage extends NioSocketMessage {

  private final Peer peer;

  public ChokeMessage(Peer peer) {
    this.peer = peer;
  }

  @Override
  protected ByteBuffer convertToBytes() {
    return null;
  }

  @Override
  public void process() {
    // handle choke message.
    peer.choke();
  }
}
