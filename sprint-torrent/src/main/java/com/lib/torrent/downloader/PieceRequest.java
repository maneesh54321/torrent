package com.lib.torrent.downloader;

import com.lib.torrent.parser.MetaInfo;

public class PieceRequest {

  private static final int BLOCK_SIZE = 16_384;
  private int index;
  private BlockRequest[] blockRequests;
  private DownloadedBlock[] downloadedBlocks;

  private int totalBlocks;

  private int blocksDownloaded;

  public PieceRequest(int index, MetaInfo metaInfo) {
    this.index = index;

    long pieceLength = metaInfo.getInfo().getPieceLength();
    int totalPieces = metaInfo.getInfo().getTotalPieces();
    // if it the last piece
    if (index == totalPieces - 1) {
      pieceLength = metaInfo.getInfo().getTotalSizeInBytes() % pieceLength;
    }
    totalBlocks = (int) pieceLength / BLOCK_SIZE;

    long lastBlockSize = pieceLength % BLOCK_SIZE;

    if(lastBlockSize > 0){
      this.blockRequests = new BlockRequest[totalBlocks+1];
      this.downloadedBlocks = new DownloadedBlock[totalBlocks+1];
      this.blockRequests[totalBlocks] = new BlockRequest(index, totalBlocks * BLOCK_SIZE, BLOCK_SIZE);
    } else {
      this.blockRequests = new BlockRequest[totalBlocks];
      this.downloadedBlocks = new DownloadedBlock[totalBlocks];
    }

    int blockNo = 0;
    while (blockNo < totalBlocks) {
      this.blockRequests[blockNo] = new BlockRequest(index, blockNo * BLOCK_SIZE, BLOCK_SIZE);
      blockNo++;
    }
  }

  public boolean addDownloadedBlock(DownloadedBlock downloadedBlock) {
    this.downloadedBlocks[blocksDownloaded] = downloadedBlock;
    blocksDownloaded++;
    return blocksDownloaded == this.downloadedBlocks.length;
  }

  public BlockRequest[] getBlockRequests() {
    return blockRequests;
  }

  public boolean isDownloadComplete() {
    return blocksDownloaded == totalBlocks;
  }

  public DownloadedBlock[] getDownloadedBlocks() {
    return downloadedBlocks;
  }

  public int getBlocksDownloaded() {
    return blocksDownloaded;
  }

  @Override
  public String toString() {
    return "PieceRequest{" +
        "index=" + index +
        ", totalBlocks=" + totalBlocks +
        ", blocksDownloaded=" + blocksDownloaded +
        ", isDownloadCompleted=" + isDownloadComplete() +
        '}';
  }
}
