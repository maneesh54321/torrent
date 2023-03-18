package com.maneesh.network.impl;

import com.maneesh.core.LongRunningProcess;
import com.maneesh.core.Peer;
import com.maneesh.core.Torrent;
import com.maneesh.network.ConnectionHandler;
import com.maneesh.network.PeerIOHandler;
import com.maneesh.network.state.PeerConnectionState;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NioConnectionHandler implements ConnectionHandler, LongRunningProcess {

  private static final Logger log = LoggerFactory.getLogger(NioConnectionHandler.class);

  private final int maxConcurrentConnections;
  private final ScheduledFuture<?> pollingConnectorTask;
  private final Queue<Peer> peersQueue;
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
    updateReadyConnections();
    cancelTimedOutConnections();
    // TODO enqueue failed peer connection again to peers queue to be tried again later
    enqueueNewConnections();
  }

  private void cancelTimedOutConnections() {
    Iterator<SelectionKey> keys = connected.keys().iterator();
    while (keys.hasNext()) {
      SelectionKey selectionKey = keys.next();
      PeerConnectionState peerConnectionState = (PeerConnectionState) selectionKey.attachment();
      if (Duration.between(peerConnectionState.getStartedAt(), Instant.now().minusSeconds(5))
          .isNegative()) {
        selectionKey.cancel();
      }
    }
  }

  private void updateReadyConnections() {
    try {
      connected.select(selectionKey -> {
        try {
          PeerConnectionState peerConnectionState = (PeerConnectionState) selectionKey.attachment();
          SocketChannel socket = (SocketChannel) selectionKey.channel();
          if (socket.finishConnect()) {
            onConnectionEstablished(socket, peerConnectionState.getPeer());
            selectionKey.cancel();
          }
        } catch (IOException e) {
          log.warn("Failed to connect to peer!!", e);
          selectionKey.cancel();
        }
      }, 3000);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void enqueueNewConnections() {
    PeerIOHandler peerIOHandler = torrent.getPeerIOHandler();
    while (peerIOHandler.getTotalActiveConnections() < maxConcurrentConnections
        && !peersQueue.isEmpty()) {
      Peer peer = peersQueue.poll();
      PeerConnectionState peerConnectionState = new PeerConnectionState(peer);
      try {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        try {
          socketChannel.register(connected, SelectionKey.OP_CONNECT, peerConnectionState);
          socketChannel.connect(peer.getSocketAddress());
          peerConnectionState.setStartedAt(clock.instant());
        } catch (ClosedChannelException e) {
          log.warn("Failed to establish connection with peer {}", peer, e);
        } catch (Exception e) {
          log.warn("Exception occurred while adding new connection", e);
          try {
            socketChannel.close();
          } catch (IOException ex) {
            log.error("Failed to close open channel!!! Fix this...");
          }
        }
      } catch (IOException e) {
        log.warn("Failed to create socket for peer {}", peer);
      }
    }
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
