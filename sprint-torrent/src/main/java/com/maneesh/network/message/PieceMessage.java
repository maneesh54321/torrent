package com.maneesh.network.message;

import java.nio.channels.SocketChannel;

public class PieceMessage implements IMessage {

  private int pieceIndex;

  private int offset;

  private byte[] data;

  public PieceMessage(int pieceIndex, int offset, byte[] data) {
    this.pieceIndex = pieceIndex;
    this.offset = offset;
    this.data = data;
  }

  @Override
  public void send(SocketChannel sc) {

  }

  @Override
  public void process() {

  }
}
