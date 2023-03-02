package com.lib.torrent.downloader.nonblocking;

import com.lib.torrent.common.Constants;
import com.lib.torrent.downloader.BlockRequest;
import com.lib.torrent.downloader.DownloadedBlock;
import com.lib.torrent.piece.AvailablePiece;
import com.lib.torrent.piece.AvailablePieceStore;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

public class DownloadingPiece {

  private final AvailablePieceStore availablePieceStore;
  private final Deque<BlockRequest> blockRequests;
  private AvailablePiece currentlyDownloadingPiece;
  private int downloadedBlocksCount;

  public DownloadingPiece(AvailablePieceStore availablePieceStore) {
    this.availablePieceStore = availablePieceStore;
    this.downloadedBlocksCount = 0;
    this.blockRequests = new ArrayDeque<>();
  }

  public Optional<BlockRequest> getBlockToDownload(Connection connection) {
    if (currentlyDownloadingPiece == null || downloadIsComplete()) {
      availablePieceStore.getHighestPriorityPiece().ifPresent(availablePiece -> {
        this.currentlyDownloadingPiece = availablePiece;
        this.blockRequests.clear();
        int i;
        for (i = 0; i < currentlyDownloadingPiece.getTotalBlocks() - 1; i++) {
          // TODO Consider already downloaded pieces.
          this.blockRequests.add(
              new BlockRequest(availablePiece.getPieceIndex(), i * Constants.BLOCK_SIZE,
                  Constants.BLOCK_SIZE));
        }
        if (availablePiece.getPieceLength() % Constants.BLOCK_SIZE > 0) {
          this.blockRequests.add(
              new BlockRequest(availablePiece.getPieceIndex(), i * Constants.BLOCK_SIZE,
                  (int) (availablePiece.getPieceLength() % Constants.BLOCK_SIZE)));
        } else {
          this.blockRequests.add(
              new BlockRequest(availablePiece.getPieceIndex(), i * Constants.BLOCK_SIZE,
                  Constants.BLOCK_SIZE));
        }
        // reset download count to zero
        downloadedBlocksCount = 0;
      });
    }

    Optional<BlockRequest> blockRequestOptional = Optional.ofNullable(blockRequests.peek());
    if (blockRequestOptional.isPresent()) {
      BlockRequest blockRequest = blockRequestOptional.get();
      if (connection.getBitfield().get(blockRequest.getPieceIndex())) {
        blockRequests.remove();
        return Optional.of(blockRequest);
      }
    }

    return Optional.empty();
  }

  private boolean downloadIsComplete() {
    return currentlyDownloadingPiece != null
        && downloadedBlocksCount == currentlyDownloadingPiece.getTotalBlocks();
  }

  public void completeDownload(DownloadedBlock downloadedBlock) {
    downloadedBlocksCount++;
    // TODO write the block to disk
    this.currentlyDownloadingPiece.updateBlockDownloadStatus(downloadedBlock);
  }
}
