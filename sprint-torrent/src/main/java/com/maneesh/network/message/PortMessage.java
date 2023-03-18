package com.maneesh.network.message;

import com.maneesh.core.Peer;
import java.nio.ByteBuffer;

public class PortMessage extends NioSocketMessage {

  private final Peer peer;

  public PortMessage(Peer peer) {
    this.peer = peer;
  }

  @Override
  protected ByteBuffer convertToBytes() {
    return null;
  }

  @Override
  public void process() {

  }
}
