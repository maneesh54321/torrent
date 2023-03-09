package com.maneesh.network.message;

import java.nio.channels.SocketChannel;

public class HaveMessage implements IMessage {

  private final int pieceIndex;

  public HaveMessage(int pieceIndex) {
    this.pieceIndex = pieceIndex;
  }

  @Override
  public void send(SocketChannel sc) {

  }

  @Override
  public void process() {

  }
}
