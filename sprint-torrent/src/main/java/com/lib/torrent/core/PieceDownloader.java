package com.lib.torrent.core;

public interface PieceDownloader {
    void downloadPiece(int pieceIndex, String location);
}
