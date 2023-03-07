package com.lib.torrent.core.network;

import com.lib.torrent.core.network.impl.HandshakeHandlerImpl;
import com.lib.torrent.core.network.impl.NioConnector;
import com.lib.torrent.parser.MetaInfo;
import com.lib.torrent.peers.Peer;
import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Torrent {

  private static final int MAX_CONCURRENT_CONNECTIONS = 30;

  private final MetaInfo metaInfo;

  private final HandshakeHandler handshakeHandler;

  private final PeerIOHandler peerIOHandler = null;

  private final ConnectionHandler connectionHandler;

  private final ScheduledExecutorService executorService;

  public Torrent(MetaInfo metaInfo, String peerId) {
    this.metaInfo = metaInfo;
    this.executorService = Executors.newScheduledThreadPool(3);
    Deque<Peer> peersQueue = new ArrayDeque<>();
    Clock clock = Clock.systemDefaultZone();
    this.connectionHandler = new NioConnector(executorService, MAX_CONCURRENT_CONNECTIONS, peersQueue, metaInfo);
    this.handshakeHandler = new HandshakeHandlerImpl(executorService, clock, peerId.getBytes());
//    this.peerIOHandler = new PeerIOHandler;
  }

  public MetaInfo getMetaInfo() {
    return metaInfo;
  }

  public HandshakeHandler getHandshakeHandler() {
    return handshakeHandler;
  }

  public PeerIOHandler getPeerIOHandler() {
    return peerIOHandler;
  }

  public ConnectionHandler getConnectionHandler() {
    return connectionHandler;
  }

  public ScheduledExecutorService getExecutorService() {
    return executorService;
  }
}
