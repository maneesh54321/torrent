package com.lib.torrent.downloader.nonblocking;

import com.lib.torrent.downloader.Message;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChokedState implements ConnectionState {

  private static final Logger log = LoggerFactory.getLogger(ChokedState.class);

  private final Connection connection;

  public ChokedState(Connection connection) {
    this.connection = connection;
  }

  @Override
  public ByteBuffer getRequestMessage() {
    return Message.buildInterested().flip();
  }

  @Override
  public void handleResponse(SocketChannel socketChannel) {
    try {
      verifyUnChoked(extractUnChokeMessage(socketChannel));
      log.debug("UnChoke message received!!!");
      connection.setState(connection.getReadyToDownloadState());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private ByteBuffer extractUnChokeMessage(SocketChannel socketChannel) throws IOException {
    ByteBuffer unChokeMessage = ByteBuffer.allocate(5);
    socketChannel.read(unChokeMessage);
    return unChokeMessage.flip();
  }

  private boolean verifyUnChoked(ByteBuffer unChokeMessage) {
    return unChokeMessage.getInt() == 1 && unChokeMessage.get() == 1;
  }
}
