package com.lib.torrent.downloader;

import com.lib.torrent.common.Constants;
import com.lib.torrent.common.utils.BinaryDataUtils;
import com.lib.torrent.downloader.exception.ConnectionChokedException;
import com.lib.torrent.parser.MetaInfo;
import com.lib.torrent.peers.Peer;
import com.lib.torrent.piece.DownloadPiece;
import com.lib.torrent.piece.PieceBlock;
import com.lib.torrent.piece.PieceManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PeerTCPClient implements TCPClient {

  private static final Logger log = LoggerFactory.getLogger(PeerTCPClient.class);

  private final Peer peer;

  private final MetaInfo metaInfo;

  private final PieceManager pieceManager;

  private boolean am_choking = true;

  private boolean am_interested = false;

  private boolean peer_choking = true;

  private boolean peer_interested = false;

  public PeerTCPClient(Peer peer, MetaInfo metaInfo, PieceManager pieceManager) {
    this.peer = peer;
    this.metaInfo = metaInfo;
    this.pieceManager = pieceManager;
  }

  @Override
  public void startConnection() {
    log.info("Starting connection with peer: " + peer);

    try (final Socket clientSocket = new Socket()) {
      SocketAddress socketAddress = new InetSocketAddress(peer.getIpAddress(), peer.getPort());
      clientSocket.connect(socketAddress, 3000);
      try (
          InputStream in = clientSocket.getInputStream();
          OutputStream out = clientSocket.getOutputStream();
      ) {
        out.write(
            Message.buildHandshake(metaInfo.getInfo().getInfoHash(), Constants.PEER_ID).array());

        checkIfHandshake(in);
        // handle further messages
        handleMessages(in, out);
      }
    } catch (Exception e) {
      stopConnection();
      log.error("Exception occurred while communicating with the Peer...", e);
    }
  }

  private void handleMessages(InputStream in, OutputStream out) throws Exception {
    try {
      while (true) {
        if (in.available() > 0) {
          byte[] wholeMessage = extractWholeMessage(in);
          ByteBuffer message = ByteBuffer.wrap(wholeMessage);
          MessageType messageType = MessageType.valueOf(message.get());
          switch (messageType) {
            case PIECE:
              log.info("piece message received!!!");
              // TODO write this to memory and mark this completed!!!
              int dataSize = message.remaining() - 8;
              byte[] data = new byte[dataSize];
              PieceBlock block = new PieceBlock(message.getInt(), message.getInt(),
                  message.get(data).array());
              pieceManager.complete(block);

              Optional<DownloadPiece> maybeDownloadPiece1 = pieceManager.takeDownloadPiece(
                  this.peer);
              if (maybeDownloadPiece1.isPresent()) {
                sendPieceDownloadRequest(maybeDownloadPiece1.get(), metaInfo, out);
              } else {
                // TODO If there are no pieces to download, terminate this connection.
              }
              break;
            case HAVE:
              log.info("Have message received!!!");
              // Add this peer as having the piece index received in message
              pieceManager.addDownloadPiece(
                  ByteBuffer.wrap(wholeMessage, 0, wholeMessage.length - 1).getInt(),
                  this.peer
              );
              break;
            case PIECE_REQUEST:
              log.info("piece request message received!!!");
              break;
            case CHOKE:
              log.info("choke message received!!!");
              peer_choking = true;
              throw new ConnectionChokedException("Connection has been choked!!!");
            case UNCHOKE:
              log.info("unchoke message received!!!");
              peer_choking = false;
              // send interested message
              if (!am_interested) {
                out.write(Message.buildInterested().array());
                am_interested = true;
              }
              // start downloading..
              Optional<DownloadPiece> maybeDownloadPiece = pieceManager.takeDownloadPiece(
                  this.peer);
              if (maybeDownloadPiece.isPresent()) {
                sendPieceDownloadRequest(maybeDownloadPiece.get(), metaInfo, out);
              }
              break;
            case BITFIELD:
              log.info("Bitfield message received!!!");
              // Add this peer as having all the pieces index received in message
              String bitString = BinaryDataUtils.toBinaryString(wholeMessage, 1,
                  wholeMessage.length - 1);
              for (int i = 0; i < bitString.length(); i++) {
                if (bitString.charAt(i) == '1') {
                  pieceManager.addDownloadPiece(i, this.peer);
                }
              }
              break;
            case INTERESTED:
              log.info("interested message received!!!");
              peer_interested = true;
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

  private void sendPieceDownloadRequest(DownloadPiece downloadPiece, MetaInfo metaInfo,
      OutputStream out) throws IOException {
    // split the piece in blocks of 16KB each,
    // depends on if it the last piece else piece size will be piece length mentioned in
    // torrent file.
    long pieceLength = metaInfo.getInfo().getPieceLength();
    int totalPieces = metaInfo.getInfo().getPieces().length / 20;
    if (downloadPiece.getIndex() < totalPieces - 1) {
      int currentOffset = 0;
      int currentBlockSize;
      while (currentOffset < pieceLength) {
        currentBlockSize =
            pieceLength - currentOffset >= 16_384 ? 16_384 : (int) (pieceLength - currentOffset);
        byte[] requestMessage = Message.buildDownloadRequest(
            downloadPiece.getIndex(),
            currentOffset,
            currentBlockSize
        ).array();
        out.write(requestMessage);
        currentOffset += currentBlockSize;
      }
    }
  }

  private void checkIfHandshake(InputStream in) throws Exception {
    int maybeHandshakeLength = in.read();
    if (maybeHandshakeLength == -1) {
      throw new Exception("Connection terminated from Peer!!");
    } else {
      if (maybeHandshakeLength == 19) {
        byte[] bitTorrentMessage = in.readNBytes(19);
        String maybeProtocolString = new String(bitTorrentMessage, StandardCharsets.UTF_8);
        if (maybeProtocolString.equals("BitTorrent protocol")) {
          byte[] handshakeBody = in.readNBytes(48);
          // TODO: verify if the handshake message has same info hash as ours.
          log.info("Completed handshake!!!");
        } else {
          throw new Exception("No Handshake received from Peer!!!");
        }
      }
    }
  }

  private byte[] extractWholeMessage(InputStream in)
      throws IOException {
    int length = ByteBuffer.wrap(in.readNBytes(4)).getInt();
    return in.readNBytes(length);
  }

  @Override
  public void stopConnection() {
    log.info("Peer " + this.peer + ": Connection dropped!!!");
  }

  @Override
  public void printConnectionState() {
    log.info("{} state: am_choking: {} am_interested: {} peer_choking: {} peer_interested: {}",
        peer, am_choking, am_interested, peer_choking, peer_interested);
  }

}
