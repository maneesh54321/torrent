package com.lib.torrent.piece;

import com.lib.torrent.peers.Peer;
import java.util.Optional;

public class TorrentContentPiecesManager implements PieceManager {

  @Override
  public void complete(byte[] piece) {

  }

  @Override
  public Optional<DownloadPiece> takeDownloadPiece(Peer peer) {
    return null;
  }

  @Override
  public void addDownloadPiece(int pieceIndex, Peer peer) {

  }
}
