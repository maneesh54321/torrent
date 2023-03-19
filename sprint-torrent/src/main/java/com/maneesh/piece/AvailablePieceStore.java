package com.maneesh.piece;

import com.maneesh.core.Peer;
import java.util.Optional;

public interface AvailablePieceStore {

  /**
   * Adds an available piece to be prioritized for download
   * @param pieceIndex index of the piece which is available
   * @param peer the peer which has the piece
   */
  void addAvailablePiece(int pieceIndex, Peer peer);

  /**
   * @return highest priority available piece
   */
  Optional<AvailablePiece> highestPriorityPiece();

  /**
   * Remove the peer from all available Pieces.
   * If any available piece was available only on this peer,
   * the piece is removed as it is not available anymore
   * @param peer to remove
   */
  void removePeer(Peer peer);


  /**
   * Removes the highest priority piece from store
   */
  void removeHighestPriorityPiece();

  boolean isDownloadCompleted();
}
