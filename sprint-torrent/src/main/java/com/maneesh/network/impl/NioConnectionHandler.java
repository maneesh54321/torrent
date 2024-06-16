package com.maneesh.network.impl;

import com.maneesh.core.LongRunningProcess;
import com.maneesh.core.Peer;
import com.maneesh.core.Torrent;
import com.maneesh.network.ConnectionHandler;
import com.maneesh.network.PeerIOHandler;
import com.maneesh.network.state.PeerConnectionState;
import com.maneesh.peers.PeersQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.*;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class NioConnectionHandler implements ConnectionHandler, LongRunningProcess {

  private static final Logger log = LoggerFactory.getLogger(NioConnectionHandler.class);

  private final int maxConcurrentConnections;
  private final ScheduledFuture<?> pollingConnectorTask;
  private final PeersQueue peersQueue;
  private final Selector connected;
  private final Torrent torrent;
  private final Clock clock;

  public NioConnectionHandler(int maxConcurrentConnections, Torrent torrent, Clock clock)
      throws IOException {
    this.torrent = torrent;
    this.clock = clock;
    this.maxConcurrentConnections = maxConcurrentConnections;
    this.connected = Selector.open();
    this.peersQueue = torrent.getPeersQueue();
    this.pollingConnectorTask = torrent.getScheduledExecutorService().scheduleWithFixedDelay(
        this::pollPeerConnections, 50, 50, TimeUnit.MILLISECONDS
    );
  }

  private void pollPeerConnections() {
    try {
      cancelTimedOutConnections();
      updateReadyConnections();
      // enqueue failed peer connection again to peers queue to be tried again later
      enqueueNewConnections();
    } catch (Exception e) {
      log.error("Error occurred updating queued connections!!", e);
      torrent.shutdown();
    }
  }

  private void cancelTimedOutConnections() {
    for (SelectionKey selectionKey : connected.keys()) {
      PeerConnectionState peerConnectionState = getPeerConnectionStateFromKey(selectionKey);
      if (clock.instant().minusSeconds(5).isAfter(peerConnectionState.getStartedAt())) {
        log.warn("Connection timed out for peer {}", peerConnectionState.getPeer());
        cancelPeerConnection(selectionKey);
      }
    }
  }

  private void updateReadyConnections() throws IOException {
    connected.selectNow();
    var keys = connected.selectedKeys().iterator();
    while (keys.hasNext()) {
      var selectionKey = keys.next();
      keys.remove();
      var peerConnectionState = getPeerConnectionStateFromKey(selectionKey);
      var socket = (SocketChannel) selectionKey.channel();
      try {
        if (socket.finishConnect()) {
          onConnectionEstablished(socket, peerConnectionState.getPeer());
          selectionKey.cancel();
        }
      } catch (IOException e) {
        log.warn("Failed to connect to peer {}!!", peerConnectionState.getPeer(), e);
        cancelPeerConnection(selectionKey);
      }
    }
  }

  private void enqueueNewConnections() {
    var peerIOHandler = torrent.getPeerIOHandler();
    while (requireMoreConnections(peerIOHandler) && !peersQueue.isEmpty()) {
      log.info(
          "total active connections : {}, connections in progress: {}. Enqueuing more peers..",
          peerIOHandler.getTotalActiveConnections(), connected.keys().size());
      if (haveOldFailedPeers()) {
        var peer = peersQueue.poll();
        var peerConnectionState = new PeerConnectionState(peer);
        try {
          var socketChannel = SocketChannel.open();
          socketChannel.configureBlocking(false);
          try {
            socketChannel.register(connected, SelectionKey.OP_CONNECT, peerConnectionState);
            socketChannel.connect(peer.getSocketAddress());
            peerConnectionState.setStartedAt(clock.instant());
          } catch (ClosedChannelException e) {
            log.warn("Failed to establish connection with peer {}", peer, e);
            throw e;
          } catch (Exception e) {
            log.warn("Exception occurred while adding new connection with peer {}", peer, e);
            try {
              socketChannel.close();
            } catch (IOException ex) {
              log.error("Failed to close open channel!!! Fix this...");
            }
            throw e;
          }
        } catch (IOException e) {
          log.warn("Failed to create socket for peer {}", peer);
          torrent.shutdown();
        } catch (Exception e) {
          log.error("Error occurred while opening connection with peer {}!!", peer, e);
          torrent.shutdown();
        }
      } else {
        break;
      }
    }
  }

  private boolean haveOldFailedPeers() {
    Optional<Peer> peer = Optional.ofNullable(peersQueue.peek());
    return peer.filter(
        value -> Duration.between(clock.instant(), value.getLastActive().plusSeconds(120))
            .isNegative()).isPresent();
  }

  private boolean requireMoreConnections(PeerIOHandler peerIOHandler) {
    int connectionsInProgress = connected.keys().size();
    int activeConnections = peerIOHandler.getTotalActiveConnections();
    return (connectionsInProgress + activeConnections) < maxConcurrentConnections;
  }

  private void cancelPeerConnection(SelectionKey selectionKey) {
    if (null != selectionKey) {
      Peer peer = getPeerConnectionStateFromKey(selectionKey).getPeer();
      log.warn("Cancelling peer connection {}", peer);
      peer.setLastActive();
      selectionKey.cancel();
      peersQueue.offer(peer);
      SelectableChannel socket = selectionKey.channel();
      try {
        socket.close();
      } catch (IOException e) {
        log.error("Error occurred while closing the socket {}!!! Fix this...", socket);
      }
    }
  }

  private PeerConnectionState getPeerConnectionStateFromKey(SelectionKey key) {
    return (PeerConnectionState) key.attachment();
  }

  @Override
  public void onConnectionEstablished(SocketChannel socketChannel, Peer peer) {
    log.debug("Connection established with peer {}", peer);
    // initiate handshake
    torrent.getHandshakeHandler().onConnectionEstablished(socketChannel, peer);
  }

  @Override
  public void stop() {
    pollingConnectorTask.cancel(true);
  }
}
