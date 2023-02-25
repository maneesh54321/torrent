package com.lib.torrent.downloader;

import com.lib.torrent.content.ContentManager;
import com.lib.torrent.parser.MetaInfo;
import com.lib.torrent.peers.Listener;
import com.lib.torrent.peers.Peer;
import com.lib.torrent.peers.PeersStore;
import com.lib.torrent.piece.AvailablePiece;
import com.lib.torrent.piece.AvailablePieceStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TorrentDownloader implements Downloader, Listener {

  private static final Logger log = LoggerFactory.getLogger(TorrentDownloader.class);
  private static Integer downloadedPiecesNum = 0;
  private static Boolean stopped = false;
  private final int id;
  private final PeersStore peersStore;

  private final MetaInfo metaInfo;

  private final AvailablePieceStore availablePieceStore;

  private final ContentManager contentManager;
  private final Map<Peer, PeerConnection> activePeerConnections = new ConcurrentHashMap<>();
  private ExecutorService executorService = Executors.newFixedThreadPool(5);


  public TorrentDownloader(int id, PeersStore peersStore, MetaInfo metaInfo,
      AvailablePieceStore availablePieceStore, ContentManager contentManager) {
    this.id = id;
    this.peersStore = peersStore;
    this.metaInfo = metaInfo;
    this.availablePieceStore = availablePieceStore;
    this.contentManager = contentManager;
  }

  @Override
  public void update() {
    log.info("New peers are now available!!! Query from PeersStore...");

    Set<Peer> peers = peersStore.getPeers();

    log.info("Number of Peers available: " + peers.size());

    peers.forEach(peer -> {
      if (!this.activePeerConnections.containsKey(peer)) {
        activePeerConnections.put(peer, new TCPPeerConnection(peer, metaInfo, this));
      }
    });
  }

  @Override
  public int compareTo(Listener o) {
    return 0;
  }

  @Override
  public void start() {
    while (!stopped && !isDownloadComplete()) {
      Optional<AvailablePiece> highestPriorityPiece = this.availablePieceStore.getHighestPriorityPiece();
      log.info("Piece prioritized: {}", highestPriorityPiece);
      // TODO handle none of the peers able to download.
//      highestPriorityPiece.ifPresentOrElse(availablePiece -> availablePiece.getPeers().stream()
//          .map(activePeerConnections::get)
//          .filter(peerConnection ->
//              peerConnection.canDownload() && !peerConnection.isDownloading()
//          ).findAny()
//          .ifPresentOrElse(peerConnection -> {
//            try {
//              DownloadedBlock[] downloadedBlocks = peerConnection.download(
//                  availablePiece.getPieceIndex());
//              downloadedPiecesNum++;
//              for (DownloadedBlock downloadedBlock : downloadedBlocks) {
//                contentManager.writeToDisk(downloadedBlock);
//              }
//              log.info("Wrote to disk, [no. of blocks written: {}]", downloadedBlocks.length);
//            } catch (Exception e) {
//              log.error("Download failed for available piece: {}", availablePiece, e);
//              availablePieceStore.restoreAvailablePiece(availablePiece);
//            }
//          }, () -> {
//            log.info("No peer found for downloading!!! Restoring piece...");
//            availablePieceStore.restoreAvailablePiece(availablePiece);
//          }), this::checkForAvailablePieces);

      highestPriorityPiece.ifPresentOrElse(availablePiece -> {
        List<PeerConnection> peersForDownload = availablePiece.getPeers().stream()
            .map(activePeerConnections::get)
            .filter(PeerConnection::canDownload)
            .toList();
        if (peersForDownload.isEmpty()) {
          log.info("No peer found for downloading!!! Restoring piece...");
          availablePieceStore.restoreAvailablePiece(availablePiece);
        } else {
          PieceRequest pieceRequest = new PieceRequest(availablePiece.getPieceIndex(), metaInfo);
          BlockRequest[] blockRequests = pieceRequest.getBlockRequests();
          Iterator<BlockRequest> requestIt = Arrays.stream(blockRequests).iterator();
          Iterator<PeerConnection> peersIt = peersForDownload.iterator();
          List<Future<DownloadedBlock>> downloadedBlockFutures = new ArrayList<>();
          while (requestIt.hasNext()) {
            if (!peersIt.hasNext()) {
              peersIt = peersForDownload.iterator();
            }
            BlockRequest blockRequest = requestIt.next();
            PeerConnection peerConnection = peersIt.next();
            downloadedBlockFutures.add(executorService.submit(() -> {
              try {
                return peerConnection.downloadBlock(blockRequest);
              } catch (Exception e) {
                log.error("Download failed for the block: {}, cancelling all block requests...",
                    blockRequest, e);
                downloadedBlockFutures.forEach(future -> future.cancel(false));
                throw new ExecutionException(e);
              }
            }));
          }
          try {
            for (Future<DownloadedBlock> future : downloadedBlockFutures) {
              pieceRequest.addDownloadedBlock(future.get());
            }
            downloadedPiecesNum++;
          } catch (CancellationException e) {
            log.error("{} block downloads were cancelled!!!",
                downloadedBlockFutures.stream().filter(Future::isCancelled).count());
          } catch (ExecutionException e) {
            downloadedBlockFutures.forEach(future -> future.cancel(false));
            availablePieceStore.restoreAvailablePiece(availablePiece);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        }
      }, this::checkForAvailablePieces);
    }
    shutdown();
  }

  private void checkForAvailablePieces() {
    log.info("Checking with Peers for any available pieces...");
    this.activePeerConnections.values()
        .forEach(peerConnection -> peerConnection.flushHaveMessages());
  }

  @Override
  public void stop() {
    stopped = true;
  }

  private void shutdown() {
    for (PeerConnection peerConnection : this.activePeerConnections.values()) {
      peerConnection.stop();
    }
    activePeerConnections.clear();
    executorService.shutdown();
  }

  @Override
  public boolean isDownloadComplete() {
    return downloadedPiecesNum == metaInfo.getInfo().getTotalPieces();
  }

  public void addAvailablePiece(int pieceIndex, Peer peer) {
    availablePieceStore.addAvailablePiece(pieceIndex, peer);
  }
}
