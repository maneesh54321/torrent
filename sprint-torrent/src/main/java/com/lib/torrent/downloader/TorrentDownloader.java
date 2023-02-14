package com.lib.torrent.downloader;

import com.lib.torrent.peers.Listener;
import com.lib.torrent.peers.Peer;
import com.lib.torrent.peers.PeersStore;
import java.util.HashSet;
import java.util.Set;

public class TorrentDownloader implements Listener {

  private final int id;

  private final PeersStore peersStore;

  private final Set<Peer> activePeers = new HashSet<>();

  public TorrentDownloader(int id, PeersStore peersStore) {
    this.id = id;
    this.peersStore = peersStore;
  }

  @Override
  public void update() {
    System.out.println("New peers are now available!!! Query from PeersStore...");

    Set<Peer> peers = peersStore.getPeers();


  }

  @Override
  public int compareTo(Listener o) {
    return 0;
  }
}
