package com.lib.torrent.piece;

import static org.springframework.util.Assert.isTrue;

import com.lib.torrent.peers.Peer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;

class AvailablePieceStoreTest {

  private AvailablePieceStore availablePieceStore;

  private PriorityQueue<AvailablePiece> availablePieces;

  private Map<Integer, AvailablePiece> availablePieceMap;

  @BeforeEach
  void setUp() {
    availablePieces = new PriorityQueue<>(
        Comparator.comparingInt(value -> value.getPeers().size()));
    availablePieceMap = new HashMap<>();
    availablePieceStore = new AvailablePieceStore(availablePieces, availablePieceMap);
    availablePieceStore.addAvailablePiece(0, new Peer("192.168.0.1", 1882));
    availablePieceStore.addAvailablePiece(0, new Peer("192.168.0.2", 1882));
    availablePieceStore.addAvailablePiece(1, new Peer("192.168.0.1", 1882));
    availablePieceStore.addAvailablePiece(2, new Peer("192.168.0.1", 1882));
    availablePieceStore.addAvailablePiece(2, new Peer("192.168.0.2", 1882));
    availablePieceStore.addAvailablePiece(3, new Peer("192.168.0.1", 1882));
    availablePieceStore.addAvailablePiece(3, new Peer("192.168.0.2", 1882));
    availablePieceStore.addAvailablePiece(3, new Peer("192.168.0.3", 1882));
    availablePieceStore.addAvailablePiece(3, new Peer("192.168.0.4", 1882));
  }

  @Test
  void addAvailablePiece() {
    Peer peer = new Peer("192.168.0.2", 1882);
    int pieceIndex = 0;
    availablePieceStore.addAvailablePiece(pieceIndex, peer);
    AvailablePiece availablePiece = new AvailablePiece(pieceIndex, peer);
    isTrue(availablePieces.contains(availablePiece),
        "Store must have the available piece in its set");
    isTrue(availablePieceMap.containsKey(pieceIndex) && availablePieceMap.get(pieceIndex)
        .equals(availablePiece), "Store must have the available piece in its map");
  }

  @Test
  void getHighestPriorityPiece() {
    Optional<AvailablePiece> availablePiece = availablePieceStore.getHighestPriorityPiece();

    isTrue(
        availablePiece.get().equals(new AvailablePiece(1, new Peer("192.168.0.1", 1882))),
        "The highest priority item is not correct!!");
  }

  @Test
  void restoreAvailablePiece() {
    Optional<AvailablePiece> availablePiece = availablePieceStore.getHighestPriorityPiece();
    availablePieceStore.restoreAvailablePiece(availablePiece.get());
    getHighestPriorityPiece();
  }
}