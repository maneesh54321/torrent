package com.lib.torrent.downloader.nonblocking;

import com.lib.torrent.parser.MetaInfo;
import com.lib.torrent.peers.Peer;
import com.lib.torrent.piece.AvailablePieceStore;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.BitSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Connection {

  private static final Logger log = LoggerFactory.getLogger(Connection.class);

  private final Peer peer;
  private final DownloadingPiece downloadingPiece;
  private ConnectionState state;
  private ConnectionState noHandshakeState;
  private ConnectionState chokedState;
  private ConnectionState readyToDownloadState;
  private ConnectionState noBitFieldState;
  private boolean am_choking = true;
  private boolean peer_interested = false;
  private BitSet bitfield;

  public Connection(Peer peer, MetaInfo metaInfo, DownloadingPiece downloadingPiece,
      AvailablePieceStore availablePieceStore) {
    this.peer = peer;
    this.noHandshakeState = new NoHandshakeState(this, metaInfo);
    this.chokedState = new ChokedState(this);
    this.readyToDownloadState = new ReadyToDownloadState(this, availablePieceStore);
    this.noBitFieldState = new NoBitFieldState(this, availablePieceStore);
    this.state = noHandshakeState;
    this.downloadingPiece = downloadingPiece;
  }

  public ByteBuffer getRequestMessage() {
    return this.state.getRequestMessage();
  }

  public void handleResponse(SocketChannel socketChannel) throws Exception {
    try {
      this.state.handleResponse(socketChannel);
    } catch (Exception e) {
      log.error("Error occurred while handling response", e);
      throw e;
    }
  }

  public void setState(ConnectionState state) {
    this.state = state;
  }

  public ConnectionState getNoHandshakeState() {
    return noHandshakeState;
  }

  public ConnectionState getChokedState() {
    return chokedState;
  }

  public ConnectionState getReadyToDownloadState() {
    return readyToDownloadState;
  }

  public ConnectionState getNoBitFieldState() {
    return noBitFieldState;
  }

  public Peer getPeer() {
    return peer;
  }

  public BitSet getBitfield() {
    return bitfield;
  }

  public DownloadingPiece getDownloadingPiece() {
    return downloadingPiece;
  }

  public void set_peer_interested(boolean peer_interested) {
    this.peer_interested = peer_interested;
  }

  public void setBitfield(ByteBuffer byteBuffer) {
    this.bitfield = BitSet.valueOf(byteBuffer);
  }

  public void addPiece(int index) {
    this.bitfield.set(index);
  }

  public boolean hasPiece(int index) {
    return this.bitfield.get(index);
  }

  public void reset() {
    this.state = this.getNoHandshakeState();
  }
}
