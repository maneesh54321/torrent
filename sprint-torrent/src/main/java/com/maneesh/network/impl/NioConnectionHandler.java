package com.maneesh.network.impl;

import com.maneesh.core.LongRunningProcess;
import com.maneesh.core.Peer;
import com.maneesh.core.Torrent;
import com.maneesh.network.ConnectionHandler;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.Set;
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

  public NioConnectionHandler(int maxConcurrentConnections, Torrent torrent) throws IOException {
    this.torrent = torrent;
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

  }

  private void updateReadyConnections() {
    try {
      connected.select(selectionKey -> {
        if (selectionKey.isConnectable()) {
          try {
            SocketChannel sc = (SocketChannel) selectionKey.channel();
            if (sc.finishConnect()) {
              Peer peer = (Peer) selectionKey.attachment();
              onConnectionEstablished(sc, peer);
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
      SocketChannel socketChannel = null;
      try {
        socketChannel = SocketChannel.open();
        socketChannel.register(connected, SelectionKey.OP_CONNECT, peer);
        socketChannel.connect(peer.getSocketAddress());
      } catch (ClosedChannelException e) {
        log.warn("Failed to open socket for peer {}", peer, e);
      } catch (IOException e) {
        try {
          assert socketChannel != null;
          socketChannel.close();
        } catch (IOException ex) {
          log.error("Failed to close open channel!!! Fix this...");
        }
      }
    }
  }

  @Override
  public void onConnectionEstablished(SocketChannel socketChannel, Peer peer) {
    // initiate handshake
    torrent.getHandshakeHandler().initiateHandshake(socketChannel, peer);
  }

  @Override
  public void stop() {
    pollingConnectorTask.cancel(true);
  }
}
