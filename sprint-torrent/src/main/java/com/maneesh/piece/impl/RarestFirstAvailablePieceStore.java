package com.maneesh.piece.impl;

import com.maneesh.core.Peer;
import com.maneesh.piece.AvailablePiece;
import com.maneesh.piece.AvailablePieceStore;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;

public class RarestFirstAvailablePieceStore implements AvailablePieceStore {

  private final PriorityQueue<AvailablePiece> store;

  private final Map<Integer, AvailablePiece> availablePieces;

  public RarestFirstAvailablePieceStore() {
    Comparator<AvailablePiece> comparator = Comparator.comparingInt(
        availablePiece -> availablePiece.getPeers().size());
    this.store = new PriorityQueue<>(comparator);
    this.availablePieces = new HashMap<>();
  }

  @Override
  public void addAvailablePiece(int pieceIndex, Peer peer) {
    if (availablePieces.containsKey(pieceIndex)) {
      availablePieces.get(pieceIndex).addPeer(peer);
    } else {
      AvailablePiece availablePiece = new AvailablePiece(pieceIndex, peer);
      availablePieces.put(pieceIndex, availablePiece);
      store.add(availablePiece);
    }
  }

  @Override
  public Optional<AvailablePiece> highestPriorityPiece() {
    return Optional.ofNullable(store.peek());
  }

  @Override
  public void removePeer(Peer peer) {
    availablePieces.values().forEach(availablePiece -> availablePiece.getPeers().remove(peer));
  }

  @Override
  public void removeHighestPriorityPiece() {
    store.remove();
  }
}
