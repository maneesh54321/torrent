package com.lib.torrent.piece;

import com.lib.torrent.peers.Peer;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AvailablePiece {

  private int pieceIndex;

  private Set<Peer> peers;

  private ReadWriteLock readWriteLock;

  private Lock readLock;

  private Lock writeLock;

  public AvailablePiece(int pieceIndex, Peer peer) {
    this.pieceIndex = pieceIndex;
    this.peers = new HashSet<>();
    this.peers.add(peer);
    readWriteLock = new ReentrantReadWriteLock();
    readLock = readWriteLock.readLock();
    writeLock = readWriteLock.writeLock();
  }

  public int getPieceIndex() {
    return pieceIndex;
  }

  public Set<Peer> getPeers() {
    try {
      readLock.lock();
      return peers;
    } finally {
      readLock.unlock();
    }
  }

  public boolean addPeer(Peer peer) {
    try {
      writeLock.lock();
      return peers.add(peer);
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public String toString() {
    return "AvailablePiece{" +
        "pieceIndex=" + pieceIndex +
        ", peers=" + peers +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AvailablePiece that = (AvailablePiece) o;
    return pieceIndex == that.pieceIndex;
  }

  @Override
  public int hashCode() {
    return Objects.hash(pieceIndex);
  }
}
