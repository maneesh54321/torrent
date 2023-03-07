package com.maneesh.network;

import com.lib.torrent.core.network.message.BlockMessage;
import com.lib.torrent.parser.MetaInfo;
import java.net.SocketAddress;
import java.util.Deque;

public class PeerConnection {

  private final MetaInfo metaInfo;

  private final Deque<BlockMessage> blockQueue;

  public PeerConnection(MetaInfo metaInfo, Deque<BlockMessage> blockQueue, SocketAddress socketAddress) {
    this.metaInfo = metaInfo;
    this.blockQueue = blockQueue;
    this.socketAddress = socketAddress;
  }

  public MetaInfo getMetaInfo() {
    return metaInfo;
  }

  public Deque<BlockMessage> getBlockQueue(){
    return blockQueue;
  }

  private final SocketAddress socketAddress;


  public SocketAddress getSocketAddress() {
    return socketAddress;
  }
}
