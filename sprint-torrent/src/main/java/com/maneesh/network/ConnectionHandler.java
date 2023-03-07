package com.maneesh.network;

import java.nio.channels.SocketChannel;

public interface ConnectionHandler {
  void onConnectionEstablished(SocketChannel socketChannel);
}
