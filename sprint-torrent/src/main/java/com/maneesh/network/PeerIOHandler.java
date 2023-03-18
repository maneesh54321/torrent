package com.maneesh.network;

import com.maneesh.core.Peer;
import java.nio.channels.SocketChannel;

public interface PeerIOHandler {

  void registerConnection(SocketChannel socketChannel, Peer peer) throws Exception;

  int getTotalActiveConnections();
}
