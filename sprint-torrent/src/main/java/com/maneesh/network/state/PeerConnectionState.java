package com.maneesh.network.state;

import com.maneesh.core.Peer;
import java.time.Instant;

public class PeerConnectionState {

  private final Peer peer;

  private Instant startedAt;

  public PeerConnectionState(Peer peer) {
    this.peer = peer;
  }

  public Peer getPeer() {
    return peer;
  }

  public void setStartedAt(Instant startedAt) {
    this.startedAt = startedAt;
  }

  public Instant getStartedAt() {
    return startedAt;
  }
}
