package com.lib.torrent.core;

import java.util.List;

public class TorrentPiecesManager {

    private boolean[] piecesAvailable;

    private List<PeerConnection>[] piecePeers;

    public TorrentPiecesManager(int numberOfPieces) {
        this.piecesAvailable = new boolean[numberOfPieces];
        this.piecePeers = new List[numberOfPieces];
    }

    public boolean isDownloadCompleted(){
        return false;
    }
}
