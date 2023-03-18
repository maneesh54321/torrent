package com.maneesh.network.impl;

import com.maneesh.core.LongRunningProcess;
import com.maneesh.core.Peer;
import com.maneesh.core.Torrent;
import com.maneesh.network.PeerIOHandler;
import com.maneesh.network.exception.BitTorrentProtocolViolationException;
import com.maneesh.network.message.IMessage;
import com.maneesh.network.message.MessageFactory;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PeerNioIOHandler implements PeerIOHandler, LongRunningProcess {

  private static final Logger log = LoggerFactory.getLogger(PeerNioIOHandler.class);

  private final Torrent torrent;

  private final Selector selector;

  private final ScheduledFuture<?> ioPollingTask;

  private final MessageFactory messageFactory;

  public PeerNioIOHandler(Torrent torrent, MessageFactory messageFactory) throws IOException {
    this.messageFactory = messageFactory;
    selector = Selector.open();
    this.torrent = torrent;
    ioPollingTask = torrent.getScheduledExecutorService()
        .scheduleAtFixedRate(this::pollConnectionsIO, 50, 50, TimeUnit.MILLISECONDS);
  }

  private void pollConnectionsIO() {
    try {
      selector.selectNow();
      Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
      while (keys.hasNext()) {
        SelectionKey selectionKey = keys.next();
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        Peer peer = (Peer) selectionKey.attachment();
        try {
          if (selectionKey.isReadable()) {
            while (peer.canRead(socketChannel)) {
              IMessage message = peer.readMessage();
              onDataAvailable(message);
            }
          }
          if (selectionKey.isWritable()) {
            Optional<IMessage> blockRequestMessage = Optional.ofNullable(
                peer.getBlockMessageQueue().poll());
            if (blockRequestMessage.isPresent()) {
              blockRequestMessage.get().send(socketChannel);
            }
          }
        } catch (IOException | BitTorrentProtocolViolationException e) {
          log.error("Error occurred while reading from socket in peer {}", peer, e);
          selectionKey.cancel();
        }
        keys.remove();
      }
    } catch (Exception e) {
      log.error("Error occurred while handling I/O", e);
    }
  }

  @Override
  public void registerConnection(SocketChannel socketChannel, Peer peer)
      throws ClosedChannelException {
    log.debug("Registering peer {} for IO", peer);
    socketChannel.register(selector, SelectionKey.OP_READ, peer)
        .interestOpsOr(SelectionKey.OP_WRITE);
    try {
      messageFactory.buildInterested(peer).send(socketChannel);
      peer.interested();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void onDataAvailable(IMessage message) {
    log.debug("Data available {}", message);
    message.process();
  }

  @Override
  public void stop() {
    ioPollingTask.cancel(true);
  }
}
