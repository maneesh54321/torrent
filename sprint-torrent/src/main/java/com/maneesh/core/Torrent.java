package com.maneesh.core;

import com.dampcake.bencode.Bencode;
import com.maneesh.meta.TorrentMetadata;
import com.maneesh.network.ConnectionHandler;
import com.maneesh.network.HandshakeHandler;
import com.maneesh.network.PeerIOHandler;
import com.maneesh.network.impl.NioConnectionHandler;
import com.maneesh.network.impl.NioHandshakeHandler;
import com.maneesh.network.impl.PeerNioIOHandler;
import com.maneesh.network.message.MessageFactory;
import com.maneesh.peers.impl.TorrentPeersCollector;
import com.maneesh.peers.impl.TorrentPeersSwarm;
import com.maneesh.piece.AvailablePieceStore;
import com.maneesh.piece.PieceDownloadScheduler;
import com.maneesh.piece.impl.RarestFirstAvailablePieceStore;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Clock;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Torrent {

  public static final Bencode BENCODE = new Bencode();
  private final Queue<Peer> peersQueue;
  private final ScheduledExecutorService scheduledExecutorService;
  private long uploaded;
  private long downloaded;
  private long left;
  private final TorrentMetadata torrentMetadata;
  private final int port;
  private final String peerId;

  private final ConnectionHandler connectionHandler;

  private final HandshakeHandler handshakeHandler;

  private final PeerIOHandler peerIOHandler;

  private final LongRunningProcess pieceDownloadScheduler;

  private final AvailablePieceStore availablePieceStore;

  private final LongRunningProcess peersCollector;

  public Torrent(String torrentFileAbsolutePath) throws IOException {
    scheduledExecutorService = Executors.newScheduledThreadPool(5);
    peerId = createPeerId();
    port = 6881;
    uploaded = 0;
    downloaded = 0;
    left = 0;
    torrentMetadata = TorrentMetadata.parseTorrentFile(new FileInputStream(torrentFileAbsolutePath));
    MessageFactory messageFactory = new MessageFactory(this);
    peersQueue = new TorrentPeersSwarm(messageFactory);
    Clock clock = Clock.systemDefaultZone();
    connectionHandler = new NioConnectionHandler(30, this);
    handshakeHandler  = new NioHandshakeHandler(clock, this);
    peerIOHandler = new PeerNioIOHandler(this, messageFactory);
    availablePieceStore = new RarestFirstAvailablePieceStore();
    pieceDownloadScheduler = new PieceDownloadScheduler(this, availablePieceStore);
    peersCollector = new TorrentPeersCollector(this);
  }

  private String createPeerId(){
    return "-SP1000-uartcg486250";
  }

  public Queue<Peer> getPeersQueue() {
    return peersQueue;
  }

  public ScheduledExecutorService getScheduledExecutorService() {
    return scheduledExecutorService;
  }

  public long getUploaded() {
    return uploaded;
  }

  public long getDownloaded() {
    return downloaded;
  }

  public long getLeft() {
    return left;
  }

  public TorrentMetadata getTorrentMetadata() {
    return torrentMetadata;
  }

  public int getPort() {
    return port;
  }

  public String getPeerId() {
    return peerId;
  }

  public ConnectionHandler getConnectionHandler() {
    return connectionHandler;
  }

  public HandshakeHandler getHandshakeHandler() {
    return handshakeHandler;
  }

  public void setUploaded(long uploaded) {
    this.uploaded = uploaded;
  }

  public void setDownloaded(long downloaded) {
    this.downloaded = downloaded;
  }

  public void setLeft(long left) {
    this.left = left;
  }

  public PeerIOHandler getPeerIOHandler() {
    return peerIOHandler;
  }

  public LongRunningProcess getPieceDownloadScheduler() {
    return pieceDownloadScheduler;
  }

  public AvailablePieceStore getAvailablePieceStore() {
    return availablePieceStore;
  }

  public LongRunningProcess getPeersCollector() {
    return peersCollector;
  }
}
