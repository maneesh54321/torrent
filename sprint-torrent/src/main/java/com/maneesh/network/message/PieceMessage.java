package com.maneesh.network.message;

import com.maneesh.content.DownloadedBlock;
import com.maneesh.core.Peer;
import java.nio.ByteBuffer;
import java.util.Objects;

public class PieceMessage extends NioSocketMessage {

  private final Peer peer;

  private final int pieceIndex;
  private final int offset;
  private final byte[] data;

  public PieceMessage(Peer peer, int pieceIndex, int offset, byte[] data) {
    this.peer = peer;
    this.pieceIndex = pieceIndex;
    this.offset = offset;
    this.data = data;
  }

  @Override
  protected ByteBuffer convertToBytes() {
    return null;
  }

  @Override
  public void process() {
    DownloadedBlock downloadedBlock = new DownloadedBlock(pieceIndex, offset, data);
    peer.completeBlockDownload(downloadedBlock);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PieceMessage that = (PieceMessage) o;
    return pieceIndex == that.pieceIndex && offset == that.offset;
  }

  @Override
  public int hashCode() {
    return Objects.hash(pieceIndex, offset);
  }
}
