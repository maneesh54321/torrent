package com.maneesh.peers;

import com.maneesh.core.Peer;
import java.net.SocketAddress;
import java.util.Collection;

public interface PeersQueue {
  void refreshPeers(Collection<SocketAddress> newPeerAddresses);

  void putBack(Peer peer);

  Peer take();
}
