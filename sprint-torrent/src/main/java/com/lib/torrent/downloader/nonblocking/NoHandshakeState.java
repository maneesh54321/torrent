package com.lib.torrent.downloader.nonblocking;

import com.lib.torrent.common.Constants;
import com.lib.torrent.downloader.Message;
import com.lib.torrent.parser.MetaInfo;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoHandshakeState implements ConnectionState {

  private static final Logger log = LoggerFactory.getLogger(NoHandshakeState.class);

  private final Connection connection;

  private final MetaInfo metaInfo;

  public NoHandshakeState(Connection connection, MetaInfo metaInfo) {
    this.connection = connection;
    this.metaInfo = metaInfo;
  }

  @Override
  public ByteBuffer getRequestMessage() {
    return Message.buildHandshake(metaInfo.getInfo().getInfoHash(), Constants.PEER_ID).flip();
  }

  @Override
  public void handleResponse(SocketChannel socketChannel) throws Exception {
    verifyHandshake(readHandshakeMessage(socketChannel));
    this.connection.setState(connection.getNoBitFieldState());
  }

  private void verifyHandshake(ByteBuffer handshake) throws Exception {
    try {
      // check if it is handshake
      if (handshake.get() == 19) {
        byte[] protocolBuffer = new byte[19];
        handshake.get(protocolBuffer);
        String maybeProtocolString = new String(protocolBuffer, 0, 19, StandardCharsets.UTF_8);
        if (maybeProtocolString.equals("BitTorrent protocol")) {
          // read empty 8 bytes
          handshake.getLong();
          // Check if the info hash matches and drop the peer if it doesn't
          byte[] peerInfoHashBuffer = new byte[20];
          handshake.get(peerInfoHashBuffer);
          if (MessageDigest.isEqual(metaInfo.getInfo().getInfoHash(), peerInfoHashBuffer)) {
            log.info("Completed handshake!!!");
            return;
          } else {
            log.error("Peer has invalid info hash!!!");
          }
        }
      }
      throw new Exception("Not a handshake message!!!");
    } catch (Exception e) {
      log.error("Handshake failed with Peer!!!", e);
      throw new Exception("Handshake failed with Peer!!!", e);
    }
  }

  private ByteBuffer readHandshakeMessage(SocketChannel channel) throws IOException {
    ByteBuffer handshakeMessage = ByteBuffer.allocate(68);
    int bytesRead = 0;
    while (bytesRead < 68){
      bytesRead += channel.read(handshakeMessage);
    }
    handshakeMessage.flip();
    return handshakeMessage;
  }
}
