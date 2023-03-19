package com.maneesh.core;

import com.dampcake.bencode.Bencode;
import com.maneesh.content.ContentManager;
import com.maneesh.content.impl.ContentManagerRandomAccessFileImpl;
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
import com.maneesh.piece.impl.PieceDownloadSchedulerImpl;
import com.maneesh.piece.impl.RarestFirstAvailablePieceStore;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Clock;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Torrent {

  public static final Bencode BENCODE = new Bencode();
  private static final Logger log = LoggerFactory.getLogger(Torrent.class);
  private final Queue<Peer> peersQueue;
  private final ScheduledExecutorService scheduledExecutorService;
  private final TorrentMetadata torrentMetadata;
  private final int port;
  private final String peerId;
  private final ConnectionHandler connectionHandler;
  private final HandshakeHandler handshakeHandler;
  private final PeerIOHandler peerIOHandler;
  private final PieceDownloadSchedulerImpl pieceDownloadScheduler;
  private final AvailablePieceStore availablePieceStore;
  private final LongRunningProcess peersCollector;
  private final ContentManager contentManager;
  private long uploaded;
  private long downloaded;
  private long left;

  public Torrent(String torrentFileAbsolutePath) throws IOException {
    scheduledExecutorService = Executors.newScheduledThreadPool(10);
    peerId = createPeerId();
    port = 6881;
    uploaded = 0;
    downloaded = 0;
    left = 0;
    torrentMetadata = TorrentMetadata.parseTorrentFile(
        new FileInputStream(torrentFileAbsolutePath));
    contentManager = new ContentManagerRandomAccessFileImpl(scheduledExecutorService,
        torrentMetadata.getInfo());
    MessageFactory messageFactory = new MessageFactory(this);
    peersQueue = new TorrentPeersSwarm(messageFactory);
    Clock clock = Clock.systemDefaultZone();
    availablePieceStore = new RarestFirstAvailablePieceStore();
    pieceDownloadScheduler = new PieceDownloadSchedulerImpl(scheduledExecutorService,
        torrentMetadata.getInfo(), contentManager, availablePieceStore);
    connectionHandler = new NioConnectionHandler(30, this, clock);
    handshakeHandler = new NioHandshakeHandler(this, clock);
    peerIOHandler = new PeerNioIOHandler(this, messageFactory);
    peersCollector = new TorrentPeersCollector(this);
  }

  private String createPeerId() {
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

  public void setUploaded(long uploaded) {
    this.uploaded = uploaded;
  }

  public long getDownloaded() {
    return downloaded;
  }

  public void setDownloaded(long downloaded) {
    this.downloaded = downloaded;
  }

  public long getLeft() {
    return left;
  }

  public void setLeft(long left) {
    this.left = left;
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

  public PeerIOHandler getPeerIOHandler() {
    return peerIOHandler;
  }

  public PieceDownloadScheduler getPieceDownloadScheduler() {
    return pieceDownloadScheduler;
  }

  public AvailablePieceStore getAvailablePieceStore() {
    return availablePieceStore;
  }

  public LongRunningProcess getPeersCollector() {
    return peersCollector;
  }

  public ContentManager getContentManager() {
    return contentManager;
  }

  public void shutdown() {
    log.warn("Shutting down torrent client!!");
    ((LongRunningProcess) getPeerIOHandler()).stop();
    ((LongRunningProcess) getConnectionHandler()).stop();
    ((LongRunningProcess) getHandshakeHandler()).stop();
    ((LongRunningProcess) getPieceDownloadScheduler()).stop();
    getPeersCollector().stop();
    getScheduledExecutorService().shutdown();
    getContentManager().shutdown();
  }
}
