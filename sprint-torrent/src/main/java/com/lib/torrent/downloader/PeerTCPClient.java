package com.lib.torrent.downloader;

import com.lib.torrent.common.Constants;
import com.lib.torrent.parser.MetaInfo;
import com.lib.torrent.peers.Peer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class PeerTCPClient implements TCPClient {

  private final Peer peer;

  private final MetaInfo metaInfo;

  public PeerTCPClient(Peer peer, MetaInfo metaInfo) {
    this.peer = peer;
    this.metaInfo = metaInfo;
  }

  @Override
  public boolean startConnection() throws IOException {
    Socket clientSocket = new Socket(peer.getIpAddress(), peer.getPort());

    OutputStream out = clientSocket.getOutputStream();

    out.write(Message.buildHandshake(metaInfo.getInfo().getInfoHash(), Constants.PEER_ID).array());

    InputStream in = clientSocket.getInputStream();

    try {
      checkIfHandshake(in);
      byte[] wholeMessage = extractWholeMessage(in);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return false;
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
        System.out.println("Completed handshake!!!");
      } else {
        throw new Exception("Invalid connection state!!!");
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
}
