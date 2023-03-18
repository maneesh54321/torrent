package com.maneesh.network.message;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class NioSocketMessage implements IMessage {

  private static final Logger log = LoggerFactory.getLogger(NioSocketMessage.class);

  public void send(SocketChannel socketChannel) throws IOException {
    log.debug("Sending message: {} to {}", this, socketChannel.getRemoteAddress());
    socketChannel.write(convertToBytes());
  }

  protected abstract ByteBuffer convertToBytes();

}
