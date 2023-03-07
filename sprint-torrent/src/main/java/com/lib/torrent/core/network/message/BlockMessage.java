package com.lib.torrent.core.network.message;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class BlockMessage implements IMessage {

  private final int pieceIndex;

  private final int offset;

  private final int length;

  public BlockMessage(int pieceIndex, int offset, int length) {
    this.pieceIndex = pieceIndex;
    this.offset = offset;
    this.length = length;
  }

  @Override
  public void send(SocketChannel sc) {
    ByteBuffer message = ByteBuffer.allocate(17);
    message.putInt(13);
    message.put((byte) 6);
    message.putInt(pieceIndex);
    message.putInt(offset);
    message.putInt(length);
    message.flip();
    try {
      sc.write(message);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void process() {

  }
}
