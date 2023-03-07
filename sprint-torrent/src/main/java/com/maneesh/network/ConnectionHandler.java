package com.maneesh.network;

import com.maneesh.core.Peer;
import java.nio.channels.SocketChannel;

public interface ConnectionHandler {
  void onConnectionEstablished(SocketChannel socketChannel, Peer peer);
}
