package com.maneesh.network;

import com.maneesh.core.Peer;
import java.nio.channels.SocketChannel;

public interface HandshakeHandler {

  void initiateHandshake(SocketChannel socketChannel, Peer peer);

}
