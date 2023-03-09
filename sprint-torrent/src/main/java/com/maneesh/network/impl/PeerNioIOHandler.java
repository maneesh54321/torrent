package com.maneesh.network.impl;

import com.maneesh.core.LongRunningProcess;
import com.maneesh.core.Peer;
import com.maneesh.core.Torrent;
import com.maneesh.network.PeerIOHandler;
import com.maneesh.network.exception.BitTorrentProtocolViolationException;
import com.maneesh.network.message.BlockMessage;
import com.maneesh.network.message.IMessage;
import com.maneesh.network.state.PeerConnectionState;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PeerNioIOHandler implements PeerIOHandler, LongRunningProcess {

  private final Torrent torrent;

  private final Selector selector;

  private final ScheduledFuture<?> ioPollingTask;

  public PeerNioIOHandler(Torrent torrent) throws IOException {
    selector = Selector.open();
    this.torrent = torrent;
    ioPollingTask = torrent.getScheduledExecutorService()
        .scheduleAtFixedRate(this::pollConnectionsIO, 50, 50, TimeUnit.MILLISECONDS);
  }

  private void pollConnectionsIO() {
    try {
      selector.select(selectionKey -> {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        if (selectionKey.isReadable()) {
          PeerConnectionState state = (PeerConnectionState) selectionKey.attachment();
          try {
            if (state.canRead(socketChannel)) {
              IMessage message = state.readMessage();
              onDataAvailable(message);
            }
          } catch (IOException | BitTorrentProtocolViolationException e) {
            throw new RuntimeException(e);
          }
        }
        if (selectionKey.isWritable()) {
          PeerConnectionState state = (PeerConnectionState) selectionKey.attachment();
          Optional<BlockMessage> blockMessage = Optional.ofNullable(state.getBlocksQueue().poll());
          blockMessage.ifPresent(message -> message.send(socketChannel));
        }
      });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void registerConnection(SocketChannel socketChannel, Peer peer)
      throws ClosedChannelException {
    PeerConnectionState peerConnectionState = new PeerConnectionState(peer);
    socketChannel.register(selector, SelectionKey.OP_READ, peerConnectionState)
        .interestOpsAnd(SelectionKey.OP_WRITE);
  }

  private void onDataAvailable(IMessage message) {

  }

  @Override
  public void stop() {
    ioPollingTask.cancel(true);
  }
}
