package com.lib.torrent.piece;

import com.lib.torrent.peers.Peer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;

public class AvailablePieceStore {

  private PriorityQueue<AvailablePiece> availablePieces;

  private Map<Integer, AvailablePiece> availablePieceMap;

  public AvailablePieceStore() {
    availablePieces = new PriorityQueue<>(Comparator.comparingInt(value -> value.getPeers().size()));
    availablePieceMap = new HashMap<>();
  }


  /**
   * This constructor is created for unit test cases.
   * @param availablePieces
   * @param availablePieceMap
   */
  public AvailablePieceStore(PriorityQueue<AvailablePiece> availablePieces,
      Map<Integer, AvailablePiece> availablePieceMap) {
    this.availablePieces = availablePieces;
    this.availablePieceMap = availablePieceMap;
  }

  public void addAvailablePiece(int pieceIndex, Peer peer) {
    if (availablePieceMap.containsKey(pieceIndex)) {
      AvailablePiece availablePiece = availablePieceMap.get(pieceIndex);
      if (availablePiece.addPeer(peer)) {
        availablePieces.remove(availablePiece);
        availablePieces.add(availablePiece);
      }
    } else {
      AvailablePiece availablePiece = new AvailablePiece(pieceIndex, peer);
      availablePieceMap.put(pieceIndex, availablePiece);
      availablePieces.add(availablePiece);
    }
  }

  public Optional<AvailablePiece> getHighestPriorityPiece() {
    AvailablePiece highPriorityPiece = this.availablePieces.poll();
    if (highPriorityPiece != null) {
      availablePieceMap.remove(highPriorityPiece.getPieceIndex());
    }
    return Optional.ofNullable(highPriorityPiece);
  }

  public void restoreAvailablePiece(AvailablePiece availablePiece) {
    if (!availablePieceMap.containsKey(availablePiece.getPieceIndex())) {
      availablePieceMap.put(availablePiece.getPieceIndex(), availablePiece);
      availablePieces.add(availablePiece);
    }
  }

}
