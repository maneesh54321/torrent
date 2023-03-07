package com.lib.torrent.core.network.state;

import com.lib.torrent.core.network.message.BlockMessage;
import java.util.Deque;

public class PeerConnectionState {

  private final Deque<BlockMessage> blocksQueue;

  public PeerConnectionState(Deque<BlockMessage> blocksQueue) {
    this.blocksQueue = blocksQueue;
  }


}
