package com.lib.torrent.downloader;

import com.lib.torrent.common.Constants;
import java.nio.ByteBuffer;
import java.util.Random;

public class Message {

  private static final Random random = new Random();

  public static ByteBuffer buildHandshake(byte[] infoHash, String peerId) {
    ByteBuffer handshakeMessage = ByteBuffer.allocate(68);
    // pstrlen
    handshakeMessage.put((byte) 19);

    //pstr
    byte[] pstr = "BitTorrent protocol".getBytes();
    handshakeMessage.put(pstr);

    //reserved
    handshakeMessage.putInt(0);
    handshakeMessage.putInt(0);

    // info_hash
    handshakeMessage.put(infoHash);

    //peer_id
    handshakeMessage.put(peerId.getBytes());

    return handshakeMessage;
  }

  public static ByteBuffer buildKeepAlive() {
    ByteBuffer keepAlive = ByteBuffer.allocate(4);
    keepAlive.putInt(0);
    return keepAlive;
  }

  public static ByteBuffer buildUnChoke() {
    ByteBuffer unChoke = ByteBuffer.allocate(5);

    // length prefix
    unChoke.putInt(1);

    //unChoke message id = 1
    unChoke.put((byte) 1);

    return unChoke;
  }

  public static ByteBuffer buildInterested() {
    ByteBuffer interested = ByteBuffer.allocate(5);
    // length prefix
    interested.putInt(1);

    //interested message id = 2
    interested.put((byte) 2);

    return interested;
  }

  public static ByteBuffer buildChoked() {
    ByteBuffer choked = ByteBuffer.allocate(5);

    // length prefix
    choked.putInt(1);
//        choked.put(new byte[]{0, 0, 0, 1});

    //choke message id = 0
    choked.put((byte) 0);

    return choked;
  }

  public static ByteBuffer buildNotInterested() {
    ByteBuffer notInterested = ByteBuffer.allocate(5);
    // length prefix
    notInterested.putInt(1);
//        notInterested.put(new byte[]{0, 0, 0, 1});

    // uninterested message id = 3
    notInterested.put((byte) 3);

    return notInterested;
  }

  public static ByteBuffer buildDownloadRequest(int index, int currentOffset,
      int currentBlockSize) {
    ByteBuffer downloadRequest = ByteBuffer.allocate(17);

    downloadRequest.putInt(13);
    downloadRequest.put((byte) 6);
    downloadRequest.putInt(index);
    downloadRequest.putInt(currentOffset);
    downloadRequest.putInt(currentBlockSize);

    return downloadRequest;
  }

  public static ByteBuffer buildConnect(int transactionId) {

    /*  Offset  Size            Name            Value
        0       64-bit integer  protocol_id     0x41727101980 // magic constant
        8       32-bit integer  action          0 // connect
        12      32-bit integer  transaction_id  // random integer
        16
    */
    ByteBuffer connectMessage = ByteBuffer.allocate(16);
    connectMessage.putLong(Long.parseLong("41727101980", 16));

    connectMessage.putInt(0);

    connectMessage.putInt(transactionId);

    return connectMessage;
  }

  public static ByteBuffer buildAnnounce(long connectionId, byte[] infoHash, int port,
      int transactionId, long uploaded, long downloaded, long left) {
    /*Offset  Size    Name    Value
      0       64-bit integer  connection_id
      8       32-bit integer  action          1 // announce
      12      32-bit integer  transaction_id
      16      20-byte string  info_hash
      36      20-byte string  peer_id
      56      64-bit integer  downloaded
      64      64-bit integer  left
      72      64-bit integer  uploaded
      80      32-bit integer  event           0 // 0: none; 1: completed; 2: started; 3: stopped
      84      32-bit integer  IP address      0 // default
      88      32-bit integer  key
      92      32-bit integer  num_want        -1 // default
      96      16-bit integer  port
      98
      */
    ByteBuffer connectMessage = ByteBuffer.allocate(98);
    connectMessage.putLong(connectionId);

    connectMessage.putInt(1);

    connectMessage.putInt(transactionId);

    connectMessage.put(infoHash);

    connectMessage.put(Constants.PEER_ID.getBytes());

    connectMessage.putLong(downloaded);

    connectMessage.putLong(left);

    connectMessage.putLong(uploaded);

    connectMessage.putInt(0);

    connectMessage.putInt(0);

    connectMessage.putInt(transactionId);

    connectMessage.putInt(-1);

    connectMessage.putShort((short) port);

    return connectMessage;
  }
}
