package com.maneesh.network.message;

import com.maneesh.core.Peer;
import com.maneesh.core.Torrent;
import java.nio.ByteBuffer;

public class HaveMessage extends NioSocketMessage {

  private final int pieceIndex;

  private final Torrent torrent;

  private final Peer peer;

  public HaveMessage(int pieceIndex, Torrent torrent, Peer peer) {
    this.pieceIndex = pieceIndex;
    this.torrent = torrent;
    this.peer = peer;
  }

  @Override
  protected ByteBuffer convertToBytes() {
    return null;
  }

  @Override
  public void process() {

  }
}
