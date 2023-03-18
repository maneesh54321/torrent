package com.maneesh.piece;

import com.maneesh.core.Peer;
import java.util.List;

public class AvailablePiece {
  private final int pieceIndex;

  private final List<Peer> peers;

  public AvailablePiece(int pieceIndex, Peer peer) {
    this.pieceIndex = pieceIndex;
    this.peers = List.of(peer);
  }

  public void addPeer(Peer peer){
    this.peers.add(peer);
  }

  public void removePeer(Peer peer){
    assert peer!= null;
    peers.remove(peer);
  }

  public int getPieceIndex() {
    return pieceIndex;
  }

  public List<Peer> getPeers() {
    return peers;
  }
}