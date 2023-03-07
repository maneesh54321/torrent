package com.lib.torrent.core.network;

import java.nio.channels.SocketChannel;

public interface ConnectionHandler {
  void onConnectionEstablished(SocketChannel socketChannel);
}
