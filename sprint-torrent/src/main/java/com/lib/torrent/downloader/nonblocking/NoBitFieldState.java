package com.lib.torrent.downloader.nonblocking;

import com.lib.torrent.piece.AvailablePieceStore;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.BitSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoBitFieldState implements ConnectionState {

  private static final Logger log = LoggerFactory.getLogger(NoBitFieldState.class);

  private final Connection connection;

  private final AvailablePieceStore availablePieceStore;

  public NoBitFieldState(Connection connection, AvailablePieceStore availablePieceStore) {
    this.connection = connection;
    this.availablePieceStore = availablePieceStore;
  }

  @Override
  public ByteBuffer getRequestMessage() {
    return null;
  }

  @Override
  public void handleResponse(SocketChannel socketChannel) throws Exception {
    ByteBuffer header = ByteBuffer.allocate(5);
    socketChannel.read(header);
    int length = header.flip().getInt();
    if (header.get() == 5) {
      log.debug("Bitfield message received!!");
      ByteBuffer messagePayload = ByteBuffer.allocate(length-1);
      socketChannel.read(messagePayload);
      messagePayload.flip();
      this.connection.setBitfield(messagePayload);
      // update the available piece store
      BitSet bitField = this.connection.getBitfield();
      for (int i = 0; i < bitField.size(); i++) {
        if (bitField.get(i)) {
          this.availablePieceStore.addAvailablePiece(i, this.connection.getPeer());
        }
      }
      this.connection.setState(this.connection.getChokedState());
    }
  }
}
