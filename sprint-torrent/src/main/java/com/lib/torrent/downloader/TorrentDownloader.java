package com.lib.torrent.downloader;

import com.lib.torrent.parser.MetaInfo;
import com.lib.torrent.peers.Listener;
import com.lib.torrent.peers.Peer;
import com.lib.torrent.peers.PeersStore;
import com.lib.torrent.piece.PieceManager;
import java.util.HashSet;
import java.util.Set;

public class TorrentDownloader implements Listener {

  private final int id;

  private final PeersStore peersStore;

  private final MetaInfo metaInfo;

  private final PieceManager pieceManager;

  private final Set<Peer> activePeers = new HashSet<>();

  public TorrentDownloader(int id, PeersStore peersStore, MetaInfo metaInfo, PieceManager pieceManager) {
    this.id = id;
    this.peersStore = peersStore;
    this.metaInfo = metaInfo;
    this.pieceManager = pieceManager;
  }

  @Override
  public void update() {
    System.out.println("New peers are now available!!! Query from PeersStore...");

    Set<Peer> peers = peersStore.getPeers();

    peers.stream().findFirst().ifPresent(peer -> {
      TCPClient tcpClient = new PeerTCPClient(peer, metaInfo, pieceManager);
      tcpClient.startConnection();
    });
  }

  @Override
  public int compareTo(Listener o) {
    return 0;
  }
}
