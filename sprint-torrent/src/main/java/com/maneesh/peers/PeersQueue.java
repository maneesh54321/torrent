package com.maneesh.peers;

import com.maneesh.core.Peer;

public interface PeersQueue {
  void offer(Peer peer);

  Peer poll();

  Peer peek();

  boolean isEmpty();
}
