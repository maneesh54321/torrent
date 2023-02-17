package com.lib.torrent.piece;

import com.lib.torrent.peers.Peer;

public class DownloadPiece {
  private int index;

  private Peer peer;

  public DownloadPiece(int index, Peer peer) {
    this.index = index;
    this.peer = peer;
  }

  public int getIndex() {
    return index;
  }
}
