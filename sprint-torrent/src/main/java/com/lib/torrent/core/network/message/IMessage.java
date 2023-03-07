package com.lib.torrent.core.network.message;

import java.nio.channels.SocketChannel;

public interface IMessage {

  void send(SocketChannel sc);

  void process();
}
