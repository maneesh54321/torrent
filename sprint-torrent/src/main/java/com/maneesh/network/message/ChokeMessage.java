package com.maneesh.network.message;

import com.maneesh.core.Peer;
import com.maneesh.network.exception.ConnectionChokedException;
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
  public void process() throws ConnectionChokedException {
    // handle choke message.
    peer.choke();
    throw new ConnectionChokedException(String.format("Peer %s has choked the connection!!", peer));
  }
}
