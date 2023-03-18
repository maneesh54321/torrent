package com.maneesh.piece;

import com.maneesh.core.LongRunningProcess;
import com.maneesh.core.Peer;
import com.maneesh.core.Torrent;
import com.maneesh.network.message.BlockRequestMessage;
import com.maneesh.network.message.IMessage;
import com.maneesh.network.message.PieceMessage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PieceDownloadScheduler implements LongRunningProcess {

  private static final Logger log = LoggerFactory.getLogger(PieceDownloadScheduler.class);

  private static final int DEFAULT_BLOCK_SIZE = 1 << 14;
  private final Torrent torrent;
  private final AvailablePieceStore availablePieceStore;
  private final ScheduledFuture<?> scheduledDownloadTask;
  private final Queue<IMessage> pendingBlockRequests;
  private boolean downloading;
  private int blocksDownloaded;
  private int totalBlocks;
  private AvailablePiece currentlyDownloadingPiece;

  public PieceDownloadScheduler(Torrent torrent, AvailablePieceStore availablePieceStore) {
    this.torrent = torrent;
    this.availablePieceStore = availablePieceStore;
    this.downloading = false;
    this.pendingBlockRequests = new ArrayDeque<>();
    this.scheduledDownloadTask = torrent.getScheduledExecutorService()
        .scheduleAtFixedRate(this::downloadPriorityPiece, 50, 50, TimeUnit.MILLISECONDS);
  }

  private void downloadPriorityPiece() {
    try{
      if (!downloading) {
        this.availablePieceStore.highestPriorityPiece().ifPresent(availablePiece -> {
          currentlyDownloadingPiece = availablePiece;
          downloading = true;
          pendingBlockRequests.addAll(createBlockRequests(availablePiece));
        });
      }
      if(currentlyDownloadingPiece != null){
        distributeBlocksToPeers(currentlyDownloadingPiece);
      }
    } catch (Exception e){
      log.warn("Exception occurred while scheduling piece download!!", e);
    }
  }

  private void distributeBlocksToPeers(AvailablePiece availablePiece) {
    // assign blocks to peers if pendingBlockRequests queue is not empty
    Iterator<Peer> peerIterator = availablePiece.getPeers().iterator();
    while (!pendingBlockRequests.isEmpty() && peerIterator.hasNext()) {
      Peer peer = peerIterator.next();
      if (peer.canDownload()) {
        peer.addMessage(pendingBlockRequests.poll());
      }
      if (pendingBlockRequests.isEmpty()) {
        break;
      }
      if (!peerIterator.hasNext()) {
        peerIterator = availablePiece.getPeers().iterator();
      }
    }
  }

  private List<IMessage> createBlockRequests(AvailablePiece availablePiece) {
    // Calculate piece length
    long totalSize = torrent.getTorrentMetadata().getInfo().getTotalSizeInBytes();
    long pieceLength = torrent.getTorrentMetadata().getInfo().getPieceLength();
    int totalPieces = torrent.getTorrentMetadata().getInfo().getInfoHash().length / 20;
    long availablePieceLength = (availablePiece.getPieceIndex() == totalPieces - 1) ?
        pieceLength : totalSize % pieceLength;
    // Create Blocks Request messages based on complete piece length and block size
    List<IMessage> blockRequests = new ArrayList<>();
    int offset = 0;
    while (availablePieceLength >= DEFAULT_BLOCK_SIZE) {
      blockRequests.add(
          new BlockRequestMessage(availablePiece.getPieceIndex(), offset, DEFAULT_BLOCK_SIZE)
      );
      availablePieceLength -= DEFAULT_BLOCK_SIZE;
      offset += DEFAULT_BLOCK_SIZE;
    }
    if (availablePieceLength > 0) {
      blockRequests.add(
          new BlockRequestMessage(
              availablePiece.getPieceIndex(), offset,
              (int) (availablePieceLength % DEFAULT_BLOCK_SIZE))
      );
    }
    totalBlocks = blockRequests.size();
    return blockRequests;
  }

  public void completeBlockDownload(PieceMessage piece) {
    // TODO Write this piece block to disk and mark the block downloaded
    // if complete piece is downloaded, set downloading = false
    blocksDownloaded++;
    if (blocksDownloaded == totalBlocks) {
      downloading = false;
    }
  }

  public void failedBlockDownloads(Collection<IMessage> messages) {
    pendingBlockRequests.addAll(messages);
  }

  @Override
  public void stop() {
    this.scheduledDownloadTask.cancel(true);
  }
}
