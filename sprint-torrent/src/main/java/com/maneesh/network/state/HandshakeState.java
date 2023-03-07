package com.maneesh.network.state;

import com.maneesh.core.Peer;
import java.nio.ByteBuffer;
import java.time.Instant;

public class HandshakeState {

  private final Peer peer;

  private final ByteBuffer messageBuffer;

  private Instant handshakeStartedAt;

  public HandshakeState(Peer peer) {
    this.peer = peer;
    messageBuffer = ByteBuffer.allocate(68);
  }

  public Peer getPeer() {
    return peer;
  }

  public ByteBuffer getMessageBuffer() {
    return messageBuffer;
  }

  public Instant getHandshakeStartedAt() {
    return handshakeStartedAt;
  }

  public void setHandshakeStartedAt(Instant handshakeStartedAt) {
    this.handshakeStartedAt = handshakeStartedAt;
  }
}
