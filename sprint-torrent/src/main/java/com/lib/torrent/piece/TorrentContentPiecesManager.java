package com.lib.torrent.piece;

import com.lib.torrent.peers.Peer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class TorrentContentPiecesManager implements PieceManager {

  private final Deque<DownloadPiece> downloadQueue;

  private Set<DownloadPiece> activeDownloadPieces;

  public TorrentContentPiecesManager() {
    this.downloadQueue = new ArrayDeque<>();
    this.activeDownloadPieces = new HashSet<>();
  }

  @Override
  public void complete(PieceBlock block) {
    System.out.println(
        "Block with Piece index: " + block.getPieceIndex() + " and offset: " + block.getOffset()
            + " downloaded!!!");
    // TODO write this block to disk.
    // TODO logic to remove piece from active download piece collection
  }

  @Override
  public Optional<DownloadPiece> takeDownloadPiece(Peer peer) {
    DownloadPiece downloadPiece = downloadQueue.poll();
    activeDownloadPieces.add(downloadPiece);
    return Optional.ofNullable(downloadPiece);
  }

  @Override
  public void addDownloadPiece(int pieceIndex, Peer peer) {
    downloadQueue.add(new DownloadPiece(pieceIndex, peer));
  }
}
