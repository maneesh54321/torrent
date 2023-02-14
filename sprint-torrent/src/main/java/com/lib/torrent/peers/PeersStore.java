package com.lib.torrent.peers;

import java.util.Collection;
import java.util.Set;

public interface PeersStore {

  Set<Peer> getPeers();

  void addPeers(Collection<Peer> peers);
}
