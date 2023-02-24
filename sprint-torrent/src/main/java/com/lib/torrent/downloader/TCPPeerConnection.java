package com.lib.torrent.downloader;

import com.lib.torrent.common.Constants;
import com.lib.torrent.common.utils.BinaryDataUtils;
import com.lib.torrent.downloader.exception.ConnectionChokedException;
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
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TCPPeerConnection implements PeerConnection {

  private static final Logger log = LoggerFactory.getLogger(TCPPeerConnection.class);

  private final Peer peer;

  private final MetaInfo metaInfo;

  private boolean am_choking = true;

  private boolean am_interested = false;

  private boolean peer_choking = true;

  private boolean peer_interested = false;

  private DataInputStream dis;

  private DataOutputStream dos;

  private PieceRequest pieceRequest;

  private TorrentDownloader torrentDownloader;

  public TCPPeerConnection(Peer peer, MetaInfo metaInfo, TorrentDownloader torrentDownloader) {
    this.peer = peer;
    this.metaInfo = metaInfo;
    this.torrentDownloader = torrentDownloader;
    start();
  }

  @Override
  public void start() {
    log.info("Starting connection with peer: " + peer);

    try {
      final Socket clientSocket = new Socket();
      SocketAddress socketAddress = new InetSocketAddress(peer.getIpAddress(), peer.getPort());
      clientSocket.connect(socketAddress, 3000);
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
  public boolean isDownloading() {
    return pieceRequest != null && !pieceRequest.isDownloadComplete();
  }

  @Override
  public boolean canDownload() {
    return am_interested && !peer_choking;
  }

  @Override
  public synchronized void download(int pieceIndex) throws Exception {
    try {
      if (am_interested && !peer_choking) {
        pieceRequest = new PieceRequest(pieceIndex, metaInfo);
        int i = 0;
        BlockRequest[] blockRequests = pieceRequest.getBlockRequests();
        while (i < blockRequests.length) {
          for (int j = i; j < i + 10; j++) {
            BlockRequest br = blockRequests[j];
            log.info("Sending request for block : {}", br);
            dos.write(
                Message.buildDownloadRequest(br.getPieceIndex(), br.getOffset(), br.getLength())
                    .array());
          }
          i += 10;

          log.debug("Receiving pieces...");
          // receive pieces
          handleMessages(() -> !pieceRequest.isDownloadComplete());
          log.info("Downloaded 10 blocks, downloadComplete? {}", pieceRequest.isDownloadComplete());
        }
        log.info("Downloaded piece: {}", pieceRequest);
      }
    } catch (Exception e) {
      throw new Exception("Download failed!!", e);
    }
  }

  private void handleMessages(Supplier<Boolean> keepReading) throws Exception {
    try {
      while (keepReading.get()) {
        if (dis.available() > 0) {
          // extract a full message
          byte[] wholeMessage = extractWholeMessage(dis);
          ByteBuffer message = ByteBuffer.wrap(wholeMessage);
          MessageType messageType = MessageType.valueOf(message.get());
          switch (messageType) {
            case PIECE:
              log.info("piece message received!!!");
              int dataSize = message.remaining() - 8;
              byte[] data = new byte[dataSize];
              pieceRequest.addDownloadedBlock(
                  new DownloadedBlock(message.getInt(), message.getInt(),
                      message.get(data).array()));
              break;
            case HAVE:
              log.info("Have message received!!!");
              // Add this peer as having the piece index received in message
              int pieceIndex = message.getInt();
              torrentDownloader.addAvailablePiece(pieceIndex, this.peer);
              break;
            case PIECE_REQUEST:
              log.info("piece request message received!!!");
              break;
            case CHOKE:
              log.info("choke message received!!!");
              peer_choking = true;
              throw new ConnectionChokedException("Connection has been choked!!!");
            case UNCHOKE:
              log.info("unChoke message received!!!");
              peer_choking = false;
              break;
            case BITFIELD:
              log.info("Bitfield message received!!!");
              // Add this peer as having all the pieces index received in message
              String bitString = BinaryDataUtils.toBinaryString(wholeMessage, 1,
                  wholeMessage.length - 1);
              for (int i = 0; i < bitString.length(); i++) {
                if (bitString.charAt(i) == '1') {
                  torrentDownloader.addAvailablePiece(i, this.peer);
                }
              }
              break;
            case INTERESTED:
              log.info("interested message received!!!");
              peer_interested = true;
              dos.write(Message.buildUnChoke().array());
              break;
            case NOT_INTERESTED:
              log.info("Not interested message received!!!");
              peer_interested = false;
              break;
            case CANCEL:
              log.info("cancel message received!!!");
              break;
          }
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (ConnectionChokedException e) {
      log.error(e.getMessage());
      throw e;
    }
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
