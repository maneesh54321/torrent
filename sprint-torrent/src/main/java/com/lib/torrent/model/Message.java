package com.lib.torrent.model;

import java.nio.ByteBuffer;

public class Message {
    public static ByteBuffer buildHandshake(byte[] infoHash, String peerId){
        ByteBuffer handshakeMessage = ByteBuffer.allocate(68);
        // pstrlen
        handshakeMessage.put(0, (byte)19);

        //pstr
        byte[] pstr = "BitTorrent protocol".getBytes();
        handshakeMessage.put(1, pstr);

        //reserved
        handshakeMessage.put(20, new byte[8]);

        // info_hash
        handshakeMessage.put(28, infoHash);

        //peer_id
        handshakeMessage.put(48, peerId.getBytes());

        return handshakeMessage;
    }

    public static ByteBuffer buildKeepAlive(){
        ByteBuffer keepAlive = ByteBuffer.allocate(4);
        // length prefix
//        keepAlive.put(new byte[]{0, 0, 0, 0});

        return keepAlive;
    }

    public static ByteBuffer buildUnchoke(){
        ByteBuffer unchoke = ByteBuffer.allocate(5);

        // length prefix
        unchoke.putInt(1);
//        unchoke.put(new byte[]{0, 0, 0, 1});

        //unchoke message id = 1
        unchoke.put((byte)1);

        return unchoke;
    }

    public static ByteBuffer buildInterested(){
        ByteBuffer interested = ByteBuffer.allocate(5);
        // length prefix
        interested.putInt(1);

        //interested message id = 2
        interested.put((byte)2);

        return interested;
    }

    public static ByteBuffer buildChoked(){
        ByteBuffer choked = ByteBuffer.allocate(5);

        // length prefix
        choked.putInt(1);
//        choked.put(new byte[]{0, 0, 0, 1});

        //choke message id = 0
        choked.put((byte)0);

        return choked;
    }

    public static ByteBuffer buildNotInterested(){
        ByteBuffer notInterested = ByteBuffer.allocate(5);
        // length prefix
        notInterested.putInt(1);
//        notInterested.put(new byte[]{0, 0, 0, 1});

        //uninterested message id = 3
        notInterested.put((byte)3);

        return notInterested;
    }

    public static ByteBuffer buildDownloadRequest() {
        ByteBuffer downloadRequest = ByteBuffer.allocate(17);

        downloadRequest.putInt(13);
        downloadRequest.put((byte)6);
        downloadRequest.putInt(0);
        downloadRequest.putInt(0);
        downloadRequest.putInt(200);

        return downloadRequest;
    }
}
