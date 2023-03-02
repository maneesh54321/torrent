package com.lib.torrent.downloader.nonblocking;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public interface ConnectionState {
  ByteBuffer getRequestMessage();

  void handleResponse(SocketChannel socketChannel) throws Exception;
}
