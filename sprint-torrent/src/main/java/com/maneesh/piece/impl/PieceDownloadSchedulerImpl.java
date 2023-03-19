package com.maneesh.piece.impl;

import com.maneesh.content.ContentManager;
import com.maneesh.content.DownloadedBlock;
import com.maneesh.core.LongRunningProcess;
import com.maneesh.core.Peer;
import com.maneesh.core.Torrent;
import com.maneesh.meta.Info;
import com.maneesh.network.message.BlockRequestMessage;
import com.maneesh.network.message.IMessage;
import com.maneesh.piece.AvailablePiece;
import com.maneesh.piece.AvailablePieceStore;
import com.maneesh.piece.PieceDownloadScheduler;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PieceDownloadSchedulerImpl implements PieceDownloadScheduler, LongRunningProcess {

  private static final Logger log = LoggerFactory.getLogger(PieceDownloadSchedulerImpl.class);

  private static final int DEFAULT_BLOCK_SIZE = 1 << 14;
  private final Info info;
  private final AvailablePieceStore availablePieceStore;
  private final ScheduledFuture<?> scheduledDownloadTask;
  private final Queue<IMessage> pendingBlockRequests;
  private final ContentManager contentManager;
  private final Torrent torrent;
  private final AtomicBoolean downloading;
  private final AtomicInteger blocksDownloaded;
  private int totalBlocks;
  private AvailablePiece currentlyDownloadingPiece;

  private final ReentrantLock lock;


  public PieceDownloadSchedulerImpl(Torrent torrent, ScheduledExecutorService executorService, Info info,
      ContentManager contentManager, AvailablePieceStore availablePieceStore) {
    this.info = info;
    this.contentManager = contentManager;
    this.torrent = torrent;
    this.availablePieceStore = availablePieceStore;
    this.downloading = new AtomicBoolean(false);
    this.blocksDownloaded = new AtomicInteger(0);
    this.lock = new ReentrantLock();
    this.pendingBlockRequests = new ArrayDeque<>();
    this.scheduledDownloadTask = executorService
        .scheduleAtFixedRate(this::downloadPriorityPiece, 50, 50, TimeUnit.MILLISECONDS);
  }

  private void downloadPriorityPiece() {
    try {
      lock.lock();
      if (!downloading.get()) {
        if(this.availablePieceStore.isDownloadCompleted()){
          torrent.shutdown();
        }
        this.availablePieceStore.highestPriorityPiece().ifPresent(availablePiece -> {
          log.debug("Highest priority piece: {}", availablePiece);
          currentlyDownloadingPiece = availablePiece;
          downloading.set(true);
          blocksDownloaded.set(0);
          pendingBlockRequests.addAll(createBlockRequests(availablePiece));
        });
      }
      if (currentlyDownloadingPiece != null) {
        distributeBlocksToPeers(currentlyDownloadingPiece);
      }
    } catch (Exception e) {
      log.warn("Exception occurred while scheduling piece download!!", e);
    } finally {
      lock.unlock();
    }
  }

  private void distributeBlocksToPeers(AvailablePiece availablePiece) {
    // assign blocks to peers if pendingBlockRequests queue is not empty
    if (!pendingBlockRequests.isEmpty()) {
      Iterator<Peer> peerIterator = availablePiece.getPeers().iterator();
      log.debug("Distributing block requests to peers for piece: {}", availablePiece);
      while (!pendingBlockRequests.isEmpty() && peerIterator.hasNext()) {
        Peer peer = peerIterator.next();
        if (peer.canDownload()) {
          IMessage blockRequest = pendingBlockRequests.poll();
          peer.addMessage(blockRequest);
          log.info("Block request {} assigned to {}", blockRequest, peer);
        }
        if (pendingBlockRequests.isEmpty()) {
          break;
        }
        if (!peerIterator.hasNext()) {
          peerIterator = availablePiece.getPeers().iterator();
        }
      }
    }
  }

  /**
   * Splits the available pieces into block download requests according to Bit torrent protocol.
   *
   * @param availablePiece The piece to be split into block requests
   * @return List of Block Request Messages which can be sent to peers.
   */
  private List<IMessage> createBlockRequests(AvailablePiece availablePiece) {
    // Calculate piece length
    long totalSize = info.getTotalSizeInBytes();
    long pieceLength = info.getPieceLength();
    int totalPieces = info.getInfoHash().length / 20;
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
    log.info("Total blocks created for piece index: {} = {}", availablePiece.getPieceIndex(),
        totalBlocks);
    return blockRequests;
  }

  @Override
  public void completeBlockDownload(DownloadedBlock downloadedBlock) {
    // Write this piece block to disk and mark the block downloaded
    contentManager.writeToDiskAsync(downloadedBlock);
    // if complete piece is downloaded, set downloading = false

    if (blocksDownloaded.incrementAndGet() == totalBlocks) {
      log.info("Completed downloading all blocks of piece {}", currentlyDownloadingPiece);
      downloading.set(false);
      availablePieceStore.removeHighestPriorityPiece();
    }
  }

  @Override
  public void failBlocksDownload(Collection<IMessage> messages, Peer peer) {
    log.debug("{} Returning {} block requests to PieceDownloadScheduler", peer, messages.size());
    // remove from available piece store as peer is not connected anymore
    availablePieceStore.removePeer(peer);
    // queue the blocks which could not be downloaded because peer disconnected
    pendingBlockRequests.addAll(messages);
  }

  @Override
  public void stop() {
    this.scheduledDownloadTask.cancel(true);
  }
}
