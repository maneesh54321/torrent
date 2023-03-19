package com.maneesh.network.state;

import com.maneesh.core.Peer;
import com.maneesh.network.message.IMessage;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Queue;

public class PeerIOState {

  private final Peer peer;

  private final Clock clock;

  private Instant lastActivity;

  private final Queue<IMessage> messageQueue;

  public PeerIOState(Peer peer, Clock clock) {
    this.peer = peer;
    this.clock = clock;
    this.messageQueue = new ArrayDeque<>();
  }

  public void setLastActivity(Instant lastActivity) {
    this.lastActivity = lastActivity;
  }

  public Peer getPeer() {
    return peer;
  }

  public Instant getLastActivity() {
    return lastActivity;
  }

  public void setLastActivity(){
    setLastActivity(clock.instant());
  }

  public void queueMessage(IMessage keepAlive) {
    this.messageQueue.offer(keepAlive);
  }

  public Queue<IMessage> getMessageQueue() {
    return messageQueue;
  }
}
