package com.maneesh.network.impl;

import static java.nio.channels.Selector.open;

import com.maneesh.core.LongRunningProcess;
import com.maneesh.core.Peer;
import com.maneesh.meta.TorrentMetadata;
import com.maneesh.network.ConnectionHandler;
import com.maneesh.network.exception.FailedToOpenSocketException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NioConnector implements ConnectionHandler, LongRunningProcess {

  private static final Logger log = LoggerFactory.getLogger(NioConnector.class);

  private final int maxConcurrentConnections;

  private final ScheduledFuture<?> pollingConnectorTask;
  private final Deque<Peer> peersQueue;
  private final Selector connected;
  private final TorrentMetadata torrentMetadata;

  public NioConnector(ScheduledExecutorService executorService, int maxConcurrentConnections,
      Deque<Peer> peersQueue, TorrentMetadata torrentMetadata) {
    this.maxConcurrentConnections = maxConcurrentConnections;
    this.peersQueue = peersQueue;
    this.torrentMetadata = torrentMetadata;
    this.pollingConnectorTask = executorService.scheduleAtFixedRate(this::pollPeerConnections, 50,
        50,
        TimeUnit.MILLISECONDS);
    try {
      this.connected = open();
    } catch (IOException e) {
      log.error("Exception occurred while opening a selector!!!");
      throw new RuntimeException(
          "Exception occurred while opening selector to manage connections!!");
    }
  }

  private static SocketChannel openSocket() throws FailedToOpenSocketException {
    SocketChannel socketChannel;
    try {
      socketChannel = SocketChannel.open();
    } catch (IOException e) {
      throw new FailedToOpenSocketException("Failed to open socket!!!", e);
    }
    return socketChannel;
  }

  private void pollPeerConnections() {
    updateReadyConnections();
    cancelTimedOutConnections();
    // TODO enqueue failed peer connection again to peers queue to be tried again later
    enqueueNewConnections();
  }

  private void cancelTimedOutConnections() {

  }

  private void updateReadyConnections() {
    try {
      connected.select(selectionKey -> {
        if (selectionKey.isConnectable()) {
          try {
            SocketChannel sc = (SocketChannel) selectionKey.channel();
            if (sc.finishConnect()) {
              onConnectionEstablished(sc);
              selectionKey.cancel();
            }
          } catch (IOException e) {
            log.warn("Failed to connect to peer!!", e);
            selectionKey.cancel();
          }
        }
      }, 3000);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void enqueueNewConnections() {
    Set<SelectionKey> keys = connected.keys();
    while (keys.size() < maxConcurrentConnections && !peersQueue.isEmpty()) {
      Peer peer = peersQueue.poll();
      SocketAddress socketAddress = peer.getSocketAddress();
      PeerConnection peerConnection = new PeerConnection(torrentMetadata, new ArrayDeque<>(),
          socketAddress);
      SocketChannel socketChannel = null;
      try {
        socketChannel = openSocket();
        socketChannel.register(connected, SelectionKey.OP_CONNECT, peerConnection);
        socketChannel.connect(socketAddress);
      } catch (FailedToOpenSocketException | ClosedChannelException e) {
        log.warn("Failed to open socket for peer {}", peer, e);
      } catch (IOException e) {
        try {
          socketChannel.close();
        } catch (IOException ex) {
          log.error("Failed to close open channel!!! Fix this...");
        }
      }
    }
  }

  @Override
  public void onConnectionEstablished(SocketChannel socketChannel) {
    // TODO initiate handshake
  }

  @Override
  public void stop() {
    pollingConnectorTask.cancel(true);
  }
}
