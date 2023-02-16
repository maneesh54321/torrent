package com.lib.torrent.piece;

import com.lib.torrent.peers.Peer;
import java.util.Optional;

public interface PieceManager {
  void complete(byte[] piece);

  Optional<DownloadPiece> takeDownloadPiece(Peer peer);

  void addDownloadPiece(int pieceIndex, Peer peer);
}
