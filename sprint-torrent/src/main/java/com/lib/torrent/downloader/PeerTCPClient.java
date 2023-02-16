package com.lib.torrent.downloader;

import com.lib.torrent.common.Constants;
import com.lib.torrent.downloader.exception.ConnectionChokedException;
import com.lib.torrent.parser.MetaInfo;
import com.lib.torrent.peers.Peer;
import com.lib.torrent.piece.DownloadPiece;
import com.lib.torrent.piece.PieceManager;
import com.lib.torrent.utils.BinaryDataUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class PeerTCPClient implements TCPClient {

  private final Peer peer;

  private final MetaInfo metaInfo;

  private final PieceManager pieceManager;

  private PeerConnectionStateEnum state;

  private boolean am_choking = true;

  private boolean am_interested = false;

  private boolean peer_choking = true;

  private boolean peer_interested = false;

  public PeerTCPClient(Peer peer, MetaInfo metaInfo, PieceManager pieceManager) {
    this.peer = peer;
    this.metaInfo = metaInfo;
    this.pieceManager = pieceManager;
    this.state = PeerConnectionStateEnum.CHOKED;
  }

  @Override
  public boolean startConnection() {
    System.out.println("Starting connection with peer: " + peer);
    try {
      Socket clientSocket = new Socket(peer.getIpAddress(), peer.getPort());

      OutputStream out = clientSocket.getOutputStream();

      out.write(
          Message.buildHandshake(metaInfo.getInfo().getInfoHash(), Constants.PEER_ID).array());

      InputStream in = clientSocket.getInputStream();

      checkIfHandshake(in);

      // check for unchoked message from Peer as this client is choked by default
      // and remain choked until proved otherwise

      handleMessages(in, out);

      // send interested message and wait for unchoke message from Peer

    } catch (Exception e) {
      stopConnection();
      e.printStackTrace();
    }
    return false;
  }

  private void handleMessages(InputStream in, OutputStream out) throws Exception {
    try {
      while (true) {
        byte[] wholeMessage = extractWholeMessage(in);
        switch (wholeMessage[0]) {
          case 0:
            System.out.println("choke message received!!!");
            peer_choking = true;
            throw new ConnectionChokedException("Connection has been choked!!!");
          case 1:
            System.out.println("unchoke message received!!!");
            peer_choking = false;
            // send interested message
            if (!am_interested) {
              out.write(Message.buildInterested().array());
              am_interested = true;
            }
            // start downloading..
            Optional<DownloadPiece> maybeDownloadPiece = pieceManager.takeDownloadPiece(this.peer);
            if (maybeDownloadPiece.isPresent()) {
              sendPieceDownloadRequest(maybeDownloadPiece.get(), metaInfo, out);
            }
            break;
          case 2:
            System.out.println("interested message received!!!");
            peer_interested = true;
            break;
          case 3:
            System.out.println("Not interested message received!!!");
            peer_interested = false;
            break;
          case 4:
            System.out.println("Have message received!!!");
            // Add this peer as having the piece index received in message
            pieceManager.addDownloadPiece(
                ByteBuffer.wrap(wholeMessage, 0, wholeMessage.length - 1).getInt(),
                this.peer
            );
            break;
          case 5:
            System.out.println("Bitfield message received!!!");
            // Add this peer as having all the pieces index received in message
            String bitString = BinaryDataUtils.toBinaryString(wholeMessage, 1,
                wholeMessage.length - 1);
            for (int i = 0; i < bitString.length(); i++) {
              if (bitString.charAt(i) == '1') {
                pieceManager.addDownloadPiece(i, this.peer);
              }
            }
            break;
          case 6:
            System.out.println("piece request message received!!!");
            break;
          case 7:
            System.out.println("piece message received!!!");
            // TODO write this to memory and mark this completed!!!
            Optional<DownloadPiece> maybeDownloadPiece1 = pieceManager.takeDownloadPiece(this.peer);
            if (maybeDownloadPiece1.isPresent()) {
              sendPieceDownloadRequest(maybeDownloadPiece1.get(), metaInfo, out);
            } else {
              // TODO If there are no pieces to download, terminate this connection.
            }
            break;
          case 8:
            System.out.println("cancel message received!!!");
            break;
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (ConnectionChokedException e) {
      e.printStackTrace();
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
    int probablyHandshakeLength = in.read();
    if (probablyHandshakeLength == 19) {
      byte[] bitTorrentMessage = new byte[19];
      in.read(bitTorrentMessage);
      String maybeProtocolString = new String(bitTorrentMessage, StandardCharsets.UTF_8);
      if (maybeProtocolString.equals("BitTorrent protocol")) {
        byte[] handshakeBody = new byte[48];
        in.read(handshakeBody);
        // TODO: verify if the handshake message has same info hash as ours.
        System.out.println("Completed handshake!!!");
      } else {
        throw new Exception("No Handshake received from Peer!!!");
      }
    }
  }

  private byte[] extractWholeMessage(InputStream in)
      throws IOException {
    byte[] lengthBytes = new byte[4];
    in.read(lengthBytes);
    int length = ByteBuffer.wrap(lengthBytes).getInt();
    byte[] wholeMessage = new byte[length];
    in.read(wholeMessage);
    return wholeMessage;
  }

  @Override
  public boolean stopConnection() {
    return false;
  }

  public PeerConnectionStateEnum getState() {
    return state;
  }
}
