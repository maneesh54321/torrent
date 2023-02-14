package com.lib.torrent.core;

import java.util.List;

public class TorrentContentDownloader {

    List<PieceDownloader> pieceDownloaderList;

    public TorrentContentDownloader(List<PieceDownloader> pieceDownloaderList) {
        this.pieceDownloaderList = pieceDownloaderList;
    }
}
