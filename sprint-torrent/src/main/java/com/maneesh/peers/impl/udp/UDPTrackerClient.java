package com.maneesh.peers.impl.udp;

import com.maneesh.core.Torrent;
import com.maneesh.peers.TrackerClient;
import com.maneesh.peers.impl.TrackerResponse;
import com.maneesh.peers.impl.udp.exception.InvalidUDPResponseException;
import com.maneesh.peers.impl.udp.exception.UDPAnnounceException;
import com.maneesh.peers.impl.udp.exception.UDPConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UDPTrackerClient implements TrackerClient {

  private static final Logger log = LoggerFactory.getLogger(UDPTrackerClient.class);

  private static final Random random = new Random();

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
      int transactionId, long uploaded, long downloaded, long left, String peerId) {
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

    connectMessage.put(peerId.getBytes());

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

  private static String readIpAddress(ByteBuffer buffer) {
    return String.format("%d.%d.%d.%d", buffer.get() & 0xFF, buffer.get() & 0xFF,
        buffer.get() & 0xFF, buffer.get() & 0xFF);
  }

  private static int readPort(ByteBuffer buffer) {
    return ((buffer.get() & 0xFF) << 8) | (buffer.get() & 0xFF);
  }

  @Override
  public Optional<TrackerResponse> requestPeers(String url, Torrent torrent) {
    try {
      DatagramSocket udpSocket = new DatagramSocket();
      URI udpUrl = URI.create(url);
      SocketAddress socketAddress = new InetSocketAddress(udpUrl.getHost(), udpUrl.getPort());
      Long connectionId = sendConnectMessage(udpSocket,
          new InetSocketAddress(udpUrl.getHost(), udpUrl.getPort()));
      return Optional.of(sendAnnounceMessage(udpSocket, connectionId, socketAddress, torrent));
    } catch (Exception e) {
      log.warn("Failed to fetch peers from tracker {}", url, e);
    }
    return Optional.empty();
  }

  private Long sendConnectMessage(DatagramSocket udpSocket,
      InetSocketAddress socketAddress) throws Exception {

    int n = 0;
    while (n < 8) {
      try {
        udpSocket.setSoTimeout(15000 * (2 ^ n));

        // create UDP packet with connect message to send
        int transactionId = random.nextInt(1000);
        DatagramPacket connectPacket = new DatagramPacket(buildConnect(transactionId).array(), 16,
            socketAddress);

        // send connect message
        udpSocket.send(connectPacket);

        // parse connect message response
        byte[] buffer = new byte[16];
        udpSocket.receive(new DatagramPacket(buffer, 16));

        ByteBuffer connectResponse = ByteBuffer.wrap(buffer);

        // verify the response

        // verify action
        int action = connectResponse.getInt();
        if (action != 0) {
          log.warn("Received action: {}, sent action: {}", action, 0);
          throw new InvalidUDPResponseException(
              "Connect response has invalid action!!");
        }
        // verify transactionId
        int receivedTxnId = connectResponse.getInt();
        if (transactionId != receivedTxnId) {
          log.warn("Received transactionId: {}, sent transactionId: {}", receivedTxnId,
              transactionId);
          throw new InvalidUDPResponseException(
              "Connect response has invalid transactionId!!!");
        }

        return connectResponse.getLong();
      } catch (SocketTimeoutException e) {
        log.warn("Connect timed out. retrying..", e);
        n++;
      }
    }
    throw new UDPConnectException("Connect message failed!!");
  }

  private TrackerResponse sendAnnounceMessage(DatagramSocket udpSocket,
      long connectionId, SocketAddress socketAddress, Torrent torrent)
      throws Exception {
    int n = 0;
    while (n < 3) {
      try {
        udpSocket.setSoTimeout(15000 * (int) Math.pow(2, n));
        // send announce message
        int transactionId = random.nextInt(1000);
        DatagramPacket announcePacket = new DatagramPacket(
            buildAnnounce(
                connectionId, torrent.getTorrentMetadata().getInfo().getInfoHash(),
                torrent.getPort(), transactionId, torrent.getUploaded(), torrent.getDownloaded(),
                torrent.getLeft(), torrent.getPeerId()).array(),
            98, socketAddress);

        udpSocket.send(announcePacket);

        // receive the response
        byte[] annResBuffer = new byte[4096];
        DatagramPacket responsePacket = new DatagramPacket(annResBuffer, annResBuffer.length);
        udpSocket.receive(responsePacket);
        ByteBuffer announceResponse = ByteBuffer.wrap(annResBuffer);

        // verify the response
        // Check whether the packet is at least 20 bytes.
        if (responsePacket.getLength() < 20) {
          throw new InvalidUDPResponseException("Expected response packet to have least size 20");
        }

        // Check whether the action is announce.
        int action = announceResponse.getInt();
        if (action != 1) {
          throw new InvalidUDPResponseException(
              String.format("Expected action 1 but received action %s", action));
        }

        // Check whether the transaction ID is equal to the one you chose.
        int receivedTxnId = announceResponse.getInt();
        if (receivedTxnId != transactionId) {
          throw new InvalidUDPResponseException(
              String.format("Expected transactionId %s but received %s", transactionId,
                  receivedTxnId));
        }


        log.debug("UDP announce message success");
        // build tracker response
        TrackerResponse trackerResponse = new TrackerResponse();
        trackerResponse.setInterval((long) announceResponse.getInt());
        trackerResponse.setLechers((long) announceResponse.getInt());
        trackerResponse.setSeeders((long) announceResponse.getInt());

        int peerListOffset = 20;
        while (peerListOffset < responsePacket.getLength()) {
          String host = readIpAddress(announceResponse);
          int port = readPort(announceResponse);

          trackerResponse.addPeer(host, port);
          peerListOffset += 6;
        }
        return trackerResponse;
      } catch (SocketException e) {
        log.warn("Connect timed out.", e);
        n++;
      }
    }
    throw new UDPAnnounceException("Exception occurred while sending announce message to tracker!!");
  }

}
