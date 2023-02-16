package com.lib.torrent.core;

import com.lib.torrent.downloader.PeerConnectionStateEnum;
import com.lib.torrent.peers.Peer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class PeerConnection implements TorrentPiecesAvailabilityUpdater, PieceDownloader {

    private PeerConnectionStateEnum connectionState;

    private Peer peer;

    private Selector selector;

    private SocketChannel client;

    public PeerConnection(Peer peer, Selector selector, SocketChannel socketChannel) {
        this.peer = peer;
        this.connectionState = PeerConnectionStateEnum.DISCONNECTED;
        this.selector = selector;

//        try {
////            initialize();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
    }

    private void initialize(SocketChannel socketChannel) throws IOException {
        client = socketChannel;
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_ACCEPT);
    }

    public void connect(){
        try {
            client.connect(new InetSocketAddress(peer.getIpAddress(), peer.getPort()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void downloadPiece(int pieceIndex, String location) {

    }

    @Override
    public void updateTorrentPiecesAvailability() {

    }

    public void handleMessage(ByteBuffer response){

    }
}
