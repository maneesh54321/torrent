package com.maneesh.peers.impl;

import com.maneesh.core.Peer;
import com.maneesh.network.message.MessageFactory;
import com.maneesh.peers.PeersQueue;
import com.maneesh.peers.PeersStore;
import com.maneesh.piece.PieceDownloadScheduler;
import java.net.SocketAddress;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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

  private final Clock clock;

  public TorrentPeersSwarm(MessageFactory messageFactory, PieceDownloadScheduler pieceDownloadScheduler, Clock clock) {
    this.messageFactory = messageFactory;
    this.pieceDownloadScheduler = pieceDownloadScheduler;
    deque = new ConcurrentLinkedDeque<>();
    activePeers = new ArrayList<>();
    this.clock = clock;
  }

  @Override
  public synchronized void refreshPeers(List<SocketAddress> newPeerAddresses) {

    log.info("Adding following addresses to swarm:");
    newPeerAddresses.forEach(address -> log.info("{}", address));

    // remove peers which are not there in new list of peers
    deque.removeIf(peer -> !newPeerAddresses.contains(peer.getSocketAddress()));

    // Add the peer to queue if it is not there in queue and active peers
    for (SocketAddress socketAddress : newPeerAddresses) {
      Peer peer = new Peer(socketAddress, messageFactory, pieceDownloadScheduler, clock);
      if (!deque.contains(peer) && !activePeers.contains(peer)) {
        peer.setLastActive(clock.instant().minus(5, ChronoUnit.MINUTES));
        deque.add(peer);
      }
    }
  }

  @Override
  public synchronized void offer(Peer peer) {
    if(activePeers.contains(peer)){
      activePeers.remove(peer);
      deque.offer(peer);
    }
  }

  @Override
  public synchronized Peer poll() {
    Peer peer = deque.poll();
    activePeers.add(peer);
    return peer;
  }

  @Override
  public Peer peek(){
    return deque.peek();
  }

  @Override
  public boolean isEmpty() {
    return this.deque.isEmpty();
  }
}
