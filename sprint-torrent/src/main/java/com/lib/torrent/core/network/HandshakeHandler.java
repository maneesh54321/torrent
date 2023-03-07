package com.lib.torrent.core.network;

import com.lib.torrent.core.network.state.HandshakeState;
import java.nio.channels.SocketChannel;

public interface HandshakeHandler {

  void initiateHandshake(SocketChannel socketChannel, PeerConnection peerConnection);

  void onHandshakeReceived(HandshakeState handshakeState);
}
