package com.lib.torrent.downloader;

import com.lib.torrent.parser.MetaInfo;
import com.lib.torrent.peers.Listener;
import com.lib.torrent.peers.Peer;
import com.lib.torrent.peers.PeersStore;
import com.lib.torrent.piece.PieceManager;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TorrentDownloader implements Listener {

  private static final Logger log = LoggerFactory.getLogger(TorrentDownloader.class);

  private final int id;

  private final PeersStore peersStore;

  private final MetaInfo metaInfo;

  private final PieceManager pieceManager;

  private final Set<Peer> activePeers = new HashSet<>();

  private final ExecutorService executorService;

  public TorrentDownloader(int id, PeersStore peersStore, MetaInfo metaInfo, PieceManager pieceManager) {
    this.id = id;
    this.peersStore = peersStore;
    this.metaInfo = metaInfo;
    this.pieceManager = pieceManager;
    executorService = Executors.newFixedThreadPool(5);
  }

  @Override
  public void update() {
    log.info("New peers are now available!!! Query from PeersStore...");

    Set<Peer> peers = peersStore.getPeers();

    log.info("Number of Peers available: " + peers.size());

    peers.stream().forEach(peer -> {
      TCPClient tcpClient = new PeerTCPClient(peer, metaInfo, pieceManager);
      activePeers.add(peer);
      executorService.submit(new PeerDownloadTask(tcpClient));
    });
  }

  @Override
  public int compareTo(Listener o) {
    return 0;
  }
}
