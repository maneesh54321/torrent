package com.lib.torrent.piece;

import com.lib.torrent.common.Constants;
import com.lib.torrent.downloader.DownloadedBlock;
import com.lib.torrent.parser.MetaInfo;
import com.lib.torrent.peers.Peer;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AvailablePiece {

  private int pieceIndex;

  private final Set<Peer> peers;

  private final Lock readLock;

  private final Lock writeLock;

  private final BitSet blocksDownloadStatus;

  private long pieceLength;

  public AvailablePiece(int pieceIndex, Peer peer, MetaInfo metaInfo) {
    this.pieceIndex = pieceIndex;
    this.peers = new HashSet<>();
    this.peers.add(peer);
    ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    readLock = readWriteLock.readLock();
    writeLock = readWriteLock.writeLock();

    pieceLength = metaInfo.getInfo().getPieceLength();
    int totalPieces = metaInfo.getInfo().getTotalPieces();
    // if it the last piece
    if (pieceIndex == totalPieces - 1) {
      pieceLength = metaInfo.getInfo().getTotalSizeInBytes() % pieceLength;
    }
    int totalBlocks = (int) pieceLength / Constants.BLOCK_SIZE;

    if ((pieceLength % Constants.BLOCK_SIZE) > 0) {
      totalBlocks++;
    }

    blocksDownloadStatus = new BitSet(totalBlocks);
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

  public void updateBlockDownloadStatus(DownloadedBlock downloadedBlock) {
    if(downloadedBlock.pieceIndex() == this.pieceIndex){
      this.blocksDownloadStatus.set(downloadedBlock.offset()/Constants.BLOCK_SIZE);
    }
  }

  public int getTotalBlocks(){
    return blocksDownloadStatus.size();
  }

  public long getPieceLength() {
    return pieceLength;
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
