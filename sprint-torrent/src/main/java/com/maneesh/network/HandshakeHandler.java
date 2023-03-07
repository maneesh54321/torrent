package com.maneesh.network;

import com.maneesh.core.Peer;
import com.maneesh.network.state.HandshakeState;
import java.nio.channels.SocketChannel;

public interface HandshakeHandler {

  void initiateHandshake(SocketChannel socketChannel, Peer peer);

  void onHandshakeReceived(SocketChannel socketChannel, HandshakeState handshakeState);
}
