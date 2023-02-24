package com.lib.torrent.downloader;

import com.lib.torrent.parser.MetaInfo;
import com.lib.torrent.peers.Listener;
import com.lib.torrent.peers.Peer;
import com.lib.torrent.peers.PeersStore;
import com.lib.torrent.piece.AvailablePiece;
import com.lib.torrent.piece.AvailablePieceStore;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TorrentDownloader implements Downloader, Listener {

  private static final Logger log = LoggerFactory.getLogger(TorrentDownloader.class);
  private static Integer downloadedPiecesNum = 0;
  private static Boolean stopped = false;
  private final int id;
  private final PeersStore peersStore;
  private final MetaInfo metaInfo;
  private final AvailablePieceStore availablePieceStore;
  private final Map<Peer, PeerConnection> activePeerConnections = new ConcurrentHashMap<>();


  public TorrentDownloader(int id, PeersStore peersStore, MetaInfo metaInfo,
      AvailablePieceStore availablePieceStore) {
    this.id = id;
    this.peersStore = peersStore;
    this.metaInfo = metaInfo;
    this.availablePieceStore = availablePieceStore;
  }

  @Override
  public void update() {
    log.info("New peers are now available!!! Query from PeersStore...");

    Set<Peer> peers = peersStore.getPeers();

    log.info("Number of Peers available: " + peers.size());

    peers.forEach(peer -> {
      if (!this.activePeerConnections.containsKey(peer)) {
        activePeerConnections.put(peer, new TCPPeerConnection(peer, metaInfo, this));
      }
    });
  }

  @Override
  public int compareTo(Listener o) {
    return 0;
  }

  @Override
  public void start() {
    while (!stopped && !isDownloadComplete()) {
      Optional<AvailablePiece> highestPriorityPiece = this.availablePieceStore.getHighestPriorityPiece();
      log.info("Piece prioritized: {}", highestPriorityPiece);
      // TODO handle none of the peers able to download.
      highestPriorityPiece.ifPresentOrElse(availablePiece -> availablePiece.getPeers().stream()
          .map(activePeerConnections::get)
          .filter(peerConnection ->
              peerConnection.canDownload() && !peerConnection.isDownloading()
          ).findAny()
          .ifPresentOrElse(peerConnection -> {
            try {
              peerConnection.download(availablePiece.getPieceIndex());
              downloadedPiecesNum++;
            } catch (Exception e) {
              log.error("Download failed for available piece: {}", availablePiece, e);
              this.availablePieceStore.restoreAvailablePiece(availablePiece);
            }
          }, () -> {
            log.info("No peer found for downloading!!! Restoring piece...");
          }), () -> checkForAvailablePieces());
    }
    shutdown();
  }

  private void checkForAvailablePieces() {
    log.info("checking for available pieces if any...");
    this.activePeerConnections.values().forEach(peerConnection -> peerConnection.flushHaveMessages());
  }

  @Override
  public void stop() {
    stopped = true;
  }

  private void shutdown(){
    for (PeerConnection peerConnection : this.activePeerConnections.values()) {
      peerConnection.stop();
    }
    activePeerConnections.clear();
  }

  @Override
  public boolean isDownloadComplete() {
    return downloadedPiecesNum == metaInfo.getInfo().getTotalPieces();
  }

  public void addAvailablePiece(int pieceIndex, Peer peer) {
    availablePieceStore.addAvailablePiece(pieceIndex, peer);
  }
}
