package com.maneesh.peers;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;

public interface PeersStore {

  void refreshPeers(List<SocketAddress> newPeerAddresses);
}
