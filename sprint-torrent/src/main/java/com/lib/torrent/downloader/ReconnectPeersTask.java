package com.lib.torrent.downloader;

import com.lib.torrent.peers.Peer;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReconnectPeersTask implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(ReconnectPeersTask.class);

  private final Map<Peer, PeerConnection> activePeerConnections;

  public ReconnectPeersTask(Map<Peer, PeerConnection> activePeerConnections) {
    this.activePeerConnections = activePeerConnections;
  }

  @Override
  public void run() {
    this.activePeerConnections.values().stream().filter(peerConnection -> !peerConnection.canDownload())
        .forEach(peerConnection -> {
          log.info("Attempting to reconnect peer..");
          peerConnection.start();
        });
  }
}
