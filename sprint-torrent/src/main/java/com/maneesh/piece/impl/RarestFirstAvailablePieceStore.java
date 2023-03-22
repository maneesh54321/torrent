package com.maneesh.piece.impl;

import com.maneesh.core.Peer;
import com.maneesh.piece.AvailablePiece;
import com.maneesh.piece.AvailablePieceStore;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RarestFirstAvailablePieceStore implements AvailablePieceStore {

  private static final Logger log = LoggerFactory.getLogger(RarestFirstAvailablePieceStore.class);

  private final PriorityQueue<AvailablePiece> store;

  private final Map<Integer, AvailablePiece> availablePieces;

  private final boolean[] pieceDownloadStatus;

  private final AtomicInteger downloadedPieceCount;

  public RarestFirstAvailablePieceStore(int totalPieces) {
    Comparator<AvailablePiece> comparator = Comparator.comparingInt(
        availablePiece -> availablePiece.getPeers().size());
    this.pieceDownloadStatus = new boolean[totalPieces];
    this.store = new PriorityQueue<>(comparator);
    this.availablePieces = new HashMap<>();
    this.downloadedPieceCount = new AtomicInteger(0);
  }

  @Override
  public synchronized void addAvailablePiece(int pieceIndex, Peer peer) {
    if(pieceIndex < pieceDownloadStatus.length && !isPieceDownloaded(pieceIndex)){
      if (availablePieces.containsKey(pieceIndex)) {
        availablePieces.get(pieceIndex).addPeer(peer);
      } else {
        AvailablePiece availablePiece = new AvailablePiece(pieceIndex, peer);
        availablePieces.put(pieceIndex, availablePiece);
        store.add(availablePiece);
      }
    }
  }

  private boolean isPieceDownloaded(int pieceIndex) {
    return pieceDownloadStatus[pieceIndex];
  }

  @Override
  public Optional<AvailablePiece> highestPriorityPiece() {
    return Optional.ofNullable(store.peek());
  }

  @Override
  public synchronized void removePeer(Peer peer) {
    availablePieces.values().forEach(availablePiece -> availablePiece.removePeer(peer));
  }

  @Override
  public synchronized void removeHighestPriorityPiece() {
    pieceDownloadStatus[store.remove().getPieceIndex()] = true;
    this.downloadedPieceCount.incrementAndGet();
  }

  @Override
  public boolean isDownloadCompleted() {
    log.debug("Completed {}/{} pieces.\r", this.downloadedPieceCount.get(), pieceDownloadStatus.length);
    return this.downloadedPieceCount.get() == pieceDownloadStatus.length;
  }
}
