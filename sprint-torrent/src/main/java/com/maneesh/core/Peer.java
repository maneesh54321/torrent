package com.maneesh.core;

import java.net.SocketAddress;

public class Peer {

  private final SocketAddress socketAddress;

  public Peer(SocketAddress socketAddress) {
    this.socketAddress = socketAddress;
  }

  public SocketAddress getSocketAddress() {
    return socketAddress;
  }
}
