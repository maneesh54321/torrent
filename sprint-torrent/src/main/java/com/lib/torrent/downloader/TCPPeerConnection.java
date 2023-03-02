package com.lib.torrent.downloader;

import com.lib.torrent.common.Constants;
import com.lib.torrent.common.utils.BinaryDataUtils;
import com.lib.torrent.downloader.exception.ConnectionChokedException;
import com.lib.torrent.downloader.exception.DownloadFailedException;
import com.lib.torrent.parser.MetaInfo;
import com.lib.torrent.peers.Peer;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TCPPeerConnection implements PeerConnection {

  private static final Logger log = LoggerFactory.getLogger(TCPPeerConnection.class);

  private final Peer peer;

  private final MetaInfo metaInfo;
  private Socket clientSocket;
  private final TorrentDownloader torrentDownloader;
  private boolean am_choking = true;
  private boolean am_interested = false;
  private boolean peer_choking = true;
  private boolean peer_interested = false;
  private DataInputStream dis;
  private DataOutputStream dos;
  private PieceRequest pieceRequest;

  public TCPPeerConnection(Peer peer, MetaInfo metaInfo, TorrentDownloader torrentDownloader) {
    this.peer = peer;
    this.metaInfo = metaInfo;
    this.torrentDownloader = torrentDownloader;
  }

  @Override
  public void start() {
    log.info("Starting connection with peer: " + peer);

    try {
      this.clientSocket = new Socket();
      SocketAddress socketAddress = new InetSocketAddress(peer.getIpAddress(), peer.getPort());
      clientSocket.connect(socketAddress, 5000);
      dis = new DataInputStream(clientSocket.getInputStream());
      dos = new DataOutputStream(clientSocket.getOutputStream());

      // send handshake message
      dos.write(
          Message.buildHandshake(metaInfo.getInfo().getInfoHash(), Constants.PEER_ID).array());

      checkIfHandshake(dis);

      // send interested message
      dos.write(Message.buildInterested().array());

      am_interested = true;

      // handle further messages
      handleMessages(() -> {
        try {
          return dis.available() > 0;
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    } catch (Exception e) {
      log.error("Exception occurred while communicating with the Peer...", e);
    }
  }

  @Override
  public boolean canDownload() {
    return am_interested && !peer_choking;
  }

  @Override
  public synchronized DownloadedBlock downloadBlock(BlockRequest blockRequest) throws Exception {
    log.debug("Sending request for block : {}", blockRequest);
    dos.write(
        Message.buildDownloadRequest(blockRequest.getPieceIndex(), blockRequest.getOffset(),
            blockRequest.getLength()).array());
    log.debug("Receiving block...");
    Supplier<Boolean> downloadUntil = () -> true;
    Optional<DownloadedBlock> downloadedBlock = receiveBlock(downloadUntil);
    log.info("Download successful!!! {}", blockRequest);
    return downloadedBlock.orElseThrow(
        () -> new DownloadFailedException("Downloaded block received is empty!!"));
  }

  private void handleMessages(Supplier<Boolean> keepReading) throws Exception {
    while (keepReading.get()) {
      if (dis.available() > 0) {
        // extract a full message
        byte[] wholeMessage = extractWholeMessage(dis);
        ByteBuffer message = ByteBuffer.wrap(wholeMessage);
        MessageType messageType = MessageType.valueOf(message.get());
        switch (messageType) {
          case PIECE -> {
            log.debug("piece message received!!!");
            int dataSize = message.remaining() - 8;
            byte[] data = new byte[dataSize];
            pieceRequest.addDownloadedBlock(
                new DownloadedBlock(message.getInt(), message.getInt(),
                    message.get(data).array()));
          }
          case HAVE -> {
            log.debug("Have message received!!!");
            // Add this peer as having the piece index received in message
            int pieceIndex = message.getInt();
            torrentDownloader.addAvailablePiece(pieceIndex, this.peer);
          }
          case PIECE_REQUEST -> log.debug("piece request message received!!!");
          case CHOKE -> {
            log.warn("choke message received!!!");
            peer_choking = true;
          }
          case UNCHOKE -> {
            log.debug("unChoke message received!!!");
            peer_choking = false;
          }
          case BITFIELD -> {
            log.debug("Bitfield message received!!!");
            // Add this peer as having all the pieces index received in message
            String bitString = BinaryDataUtils.toBinaryString(wholeMessage, 1,
                wholeMessage.length - 1);
            for (int i = 0; i < bitString.length(); i++) {
              if (bitString.charAt(i) == '1') {
                torrentDownloader.addAvailablePiece(i, this.peer);
              }
            }
          }
          case INTERESTED -> {
            log.debug("interested message received!!!");
            peer_interested = true;
            dos.write(Message.buildUnChoke().array());
          }
          case NOT_INTERESTED -> {
            log.debug("Not interested message received!!!");
            peer_interested = false;
          }
          case CANCEL -> log.debug("cancel message received!!!");
        }
      }
    }
  }

  private Optional<DownloadedBlock> receiveBlock(Supplier<Boolean> keepDownloading)
      throws Exception {
    while (keepDownloading.get()) {
      if (dis.available() > 0) {
        // extract a full message
        byte[] wholeMessage = extractWholeMessage(dis);
        ByteBuffer message = ByteBuffer.wrap(wholeMessage);
        MessageType messageType = MessageType.valueOf(message.get());
        switch (messageType) {
          case PIECE -> {
            log.debug("piece message received!!!");
            int dataSize = message.remaining() - 8;
            byte[] data = new byte[dataSize];
            return Optional.of(new DownloadedBlock(message.getInt(),
                message.getInt(), message.get(data).array()));
          }
          case HAVE -> {
            log.debug("Have message received!!!");
            // Add this peer as having the piece index received in message
            int pieceIndex = message.getInt();
            torrentDownloader.addAvailablePiece(pieceIndex, this.peer);
          }
          case PIECE_REQUEST -> log.debug("piece request message received!!!");
          case CHOKE -> {
            log.debug("choke message received!!!");
            peer_choking = true;
            throw new ConnectionChokedException("Connection has been choked!!!");
          }
          case UNCHOKE -> {
            log.debug("unChoke message received!!!");
            peer_choking = false;
          }
          case BITFIELD -> {
            log.debug("Bitfield message received!!!");
            // Add this peer as having all the pieces index received in message
            String bitString = BinaryDataUtils.toBinaryString(wholeMessage, 1,
                wholeMessage.length - 1);
            for (int i = 0; i < bitString.length(); i++) {
              if (bitString.charAt(i) == '1') {
                torrentDownloader.addAvailablePiece(i, this.peer);
              }
            }
          }
          case INTERESTED -> {
            log.info("interested message received!!!");
            peer_interested = true;
            dos.write(Message.buildUnChoke().array());
          }
          case NOT_INTERESTED -> {
            log.info("Not interested message received!!!");
            peer_interested = false;
          }
          case CANCEL -> log.info("cancel message received!!!");
        }
      }
    }
    return Optional.empty();
  }

  private void checkIfHandshake(DataInputStream dis) throws Exception {
    try {
      byte[] handshakeResponse = new byte[68];
      dis.readFully(handshakeResponse);
      // check if it is handshake
      int maybeHandshakeLength = handshakeResponse[0];
      if (maybeHandshakeLength == 19) {
        String maybeProtocolString = new String(handshakeResponse, 1, 19, StandardCharsets.UTF_8);
        if (maybeProtocolString.equals("BitTorrent protocol")) {
          // Check if the info hash matches and drop the peer if it doesn't
          byte[] peerInfoHash = new byte[20];
          System.arraycopy(handshakeResponse, 28, peerInfoHash, 0, 20);
          if (!MessageDigest.isEqual(metaInfo.getInfo().getInfoHash(), peerInfoHash)) {
            throw new Exception("Peer has invalid info hash!!!");
          }
          log.info("Completed handshake!!!");
        }
      }
    } catch (IOException e) {
      throw new Exception("Handshake failed with Peer!!!", e);
    }
  }

  private byte[] extractWholeMessage(DataInputStream dis) throws IOException {
    int length = dis.readInt();
    return dis.readNBytes(length);
  }

  @Override
  public void stop() {
    try {
      dis.close();
      dos.close();
      clientSocket.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    log.info("Peer " + this.peer + ": Connection dropped!!!");
  }

  @Override
  public void printState() {
    log.info("{} state: am_choking: {} am_interested: {} peer_choking: {} peer_interested: {}",
        peer, am_choking, am_interested, peer_choking, peer_interested);
  }

  @Override
  public void flushHaveMessages() {
    try {
      handleMessages(() -> {
        try {
          return dis.available() > 0;
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    } catch (Exception e) {
      log.error("Exception occurred while flushing connection!!!");
    }
  }
}
