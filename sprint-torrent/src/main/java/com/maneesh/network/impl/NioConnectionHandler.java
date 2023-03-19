package com.maneesh.network.impl;

import com.maneesh.core.LongRunningProcess;
import com.maneesh.core.Peer;
import com.maneesh.core.Torrent;
import com.maneesh.network.ConnectionHandler;
import com.maneesh.network.PeerIOHandler;
import com.maneesh.network.state.PeerConnectionState;
import com.maneesh.peers.PeersQueue;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    this.pollingConnectorTask = torrent.getScheduledExecutorService().scheduleAtFixedRate(
        this::pollPeerConnections, 50, 50, TimeUnit.MILLISECONDS
    );
  }

  private void pollPeerConnections() {
    try {
      updateReadyConnections();
      cancelTimedOutConnections();
      // TODO enqueue failed peer connection again to peers queue to be tried again later
      enqueueNewConnections();
    } catch (Exception e) {
      log.error("Error occurred updating queued connections!!", e);
      torrent.shutdown();
    }
  }

  private void cancelTimedOutConnections() {
    for (SelectionKey selectionKey : connected.keys()) {
      PeerConnectionState peerConnectionState = getPeerConnectionStateFromKey(selectionKey);
      if (Duration.between(Instant.now().minusSeconds(5), peerConnectionState.getStartedAt())
          .isNegative()) {
        cancelPeerConnection(selectionKey);
      }
    }
  }

  private void updateReadyConnections() throws IOException {
    connected.selectNow();
    Iterator<SelectionKey> keys = connected.selectedKeys().iterator();
    while (keys.hasNext()) {
      SelectionKey selectionKey = keys.next();
      keys.remove();
      PeerConnectionState peerConnectionState = getPeerConnectionStateFromKey(selectionKey);
      SocketChannel socket = (SocketChannel) selectionKey.channel();
      try {
        if (socket.finishConnect()) {
          onConnectionEstablished(socket, peerConnectionState.getPeer());
          selectionKey.cancel();
        }
      } catch (IOException e) {
        log.warn("Failed to connect to peer!!", e);
        cancelPeerConnection(selectionKey);
      }
    }
  }

  private void enqueueNewConnections() {
    PeerIOHandler peerIOHandler = torrent.getPeerIOHandler();
    while (requireMoreConnections(peerIOHandler) && !peersQueue.isEmpty()) {
      log.info(
          "total active connections : {}, connections in progress: {}. Enqueuing more peers..",
          peerIOHandler.getTotalActiveConnections(), connected.keys().size());
      Optional<Peer> maybePeer = Optional.ofNullable(peersQueue.poll());
      if (maybePeer.isPresent()) {
        Peer peer = maybePeer.get();
        if (Duration.between(Instant.now(), peer.getLastActive().plusSeconds(120)).isNegative()) {
          PeerConnectionState peerConnectionState = new PeerConnectionState(peer);
          try {
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            try {
              socketChannel.register(connected, SelectionKey.OP_CONNECT, peerConnectionState);
              socketChannel.connect(peer.getSocketAddress());
              peerConnectionState.setStartedAt(clock.instant());
            } catch (ClosedChannelException e) {
              log.warn("Failed to establish connection with peer {}", maybePeer, e);
            } catch (Exception e) {
              log.warn("Exception occurred while adding new connection", e);
              try {
                socketChannel.close();
              } catch (IOException ex) {
                log.error("Failed to close open channel!!! Fix this...");
              }
            }
          } catch (IOException e) {
            log.warn("Failed to create socket for peer {}", maybePeer);
          }
        }
      }
    }
  }

  private boolean requireMoreConnections(PeerIOHandler peerIOHandler) {
    int connectionsInProgress = connected.keys().size();
    int activeConnections = peerIOHandler.getTotalActiveConnections();
    return (connectionsInProgress + activeConnections) < maxConcurrentConnections;
  }

  private void cancelPeerConnection(SelectionKey selectionKey) {
    if (null != selectionKey) {
      Peer peer = getPeerConnectionStateFromKey(selectionKey).getPeer();
      peer.setLastActive();
      peersQueue.offer(peer);
      selectionKey.cancel();
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
