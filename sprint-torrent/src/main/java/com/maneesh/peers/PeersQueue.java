package com.maneesh.peers;

import com.maneesh.core.Peer;

public interface PeersQueue {
  boolean offer(Peer peer);

  Peer poll();

  boolean isEmpty();
}
