package com.lib.torrent.core.network.state;

import com.lib.torrent.core.network.PeerConnection;
import java.nio.ByteBuffer;
import java.time.Instant;

public class HandshakeState {

  private final PeerConnection peerConnection;

  private ByteBuffer messageBuffer;

  private Instant handshakeStartedAt;

  public HandshakeState(PeerConnection peerConnection) {
    this.peerConnection = peerConnection;
    messageBuffer = ByteBuffer.allocate(68);
  }

  public PeerConnection getPeerConnection() {
    return peerConnection;
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
