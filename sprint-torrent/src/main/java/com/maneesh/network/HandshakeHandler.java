package com.maneesh.network;

import com.maneesh.network.state.HandshakeState;
import java.nio.channels.SocketChannel;

public interface HandshakeHandler {

  void initiateHandshake(SocketChannel socketChannel, PeerConnection peerConnection);

  void onHandshakeReceived(HandshakeState handshakeState);
}
