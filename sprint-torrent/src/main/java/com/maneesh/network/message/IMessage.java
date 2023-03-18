package com.maneesh.network.message;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public interface IMessage {

  void send(SocketChannel sc) throws IOException;

  void process();
}
