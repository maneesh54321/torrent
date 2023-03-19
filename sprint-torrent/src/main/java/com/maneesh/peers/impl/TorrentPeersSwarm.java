package com.maneesh.peers.impl;

import com.maneesh.core.Peer;
import com.maneesh.network.message.MessageFactory;
import com.maneesh.peers.PeersQueue;
import com.maneesh.peers.PeersStore;
import com.maneesh.piece.PieceDownloadScheduler;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TorrentPeersSwarm implements PeersStore, PeersQueue {

  private static final Logger log = LoggerFactory.getLogger(TorrentPeersSwarm.class);

  private final Deque<Peer> deque;

  // to track the peers which are currently taken out of this store for download
  private final List<Peer> activePeers;

  private final MessageFactory messageFactory;

  private final PieceDownloadScheduler pieceDownloadScheduler;

  public TorrentPeersSwarm(MessageFactory messageFactory, PieceDownloadScheduler pieceDownloadScheduler) {
    this.messageFactory = messageFactory;
    this.pieceDownloadScheduler = pieceDownloadScheduler;
    deque = new ConcurrentLinkedDeque<>();
    activePeers = new ArrayList<>();
  }

  @Override
  public void refreshPeers(Collection<SocketAddress> newPeerAddresses) {

    log.debug("Adding following addresses to swarm:");
    newPeerAddresses.forEach(address -> log.debug("{}", address));

    // remove peers which are not there in new list of peers
    for (Peer peer : deque)
      if (!newPeerAddresses.contains(peer.getSocketAddress())) {
        deque.remove(peer);
      }

    // Add the peer to queue if it is not there in queue and active peers
    for (SocketAddress socketAddress : newPeerAddresses) {
      Peer peer = new Peer(socketAddress, messageFactory, pieceDownloadScheduler);
      if (!deque.contains(peer) && !activePeers.contains(peer)) {
        deque.add(peer);
      }
    }
  }

  @Override
  public boolean offer(Peer peer) {
    activePeers.remove(peer);
    return deque.offer(peer);
  }

  @Override
  public Peer poll() {
    Peer peer = deque.poll();
    activePeers.add(peer);
    return peer;
  }

  @Override
  public boolean isEmpty() {
    return this.deque.isEmpty();
  }
}
