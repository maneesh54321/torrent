package com.maneesh.peers;

import java.net.SocketAddress;
import java.util.Collection;

public interface PeersStore {

  void refreshPeers(Collection<SocketAddress> newPeerAddresses);
}
