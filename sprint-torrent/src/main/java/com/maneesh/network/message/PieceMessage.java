package com.maneesh.network.message;

import com.maneesh.content.DownloadedBlock;
import com.maneesh.core.Torrent;
import java.nio.ByteBuffer;

public class PieceMessage extends NioSocketMessage {

  private final Torrent torrent;
  private final int pieceIndex;
  private final int offset;
  private final byte[] data;

  public PieceMessage(Torrent torrent, int pieceIndex, int offset, byte[] data) {
    this.torrent = torrent;
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
    torrent.getPieceDownloadScheduler().completeBlockDownload(downloadedBlock);
  }
}
