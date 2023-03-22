package com.maneesh.network.impl;

import com.maneesh.core.LongRunningProcess;
import com.maneesh.core.Peer;
import com.maneesh.core.Torrent;
import com.maneesh.network.PeerIOHandler;
import com.maneesh.network.exception.BitTorrentProtocolViolationException;
import com.maneesh.network.message.IMessage;
import com.maneesh.network.message.MessageFactory;
import com.maneesh.network.state.PeerIOState;
import com.maneesh.peers.PeersQueue;
import com.maneesh.piece.PieceDownloadScheduler;
import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.time.Clock;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PeerNioIOHandler implements PeerIOHandler, LongRunningProcess {

  private static final Logger log = LoggerFactory.getLogger(PeerNioIOHandler.class);

  private final Torrent torrent;

  private final PieceDownloadScheduler pieceDownloadScheduler;

  private final Selector selector;

  private final ScheduledFuture<?> ioPollingTask;

  private final MessageFactory messageFactory;

  private final PeersQueue peersQueue;

  private final Clock clock;

  public PeerNioIOHandler(Torrent torrent, MessageFactory messageFactory, Clock clock)
      throws IOException {
    this.messageFactory = messageFactory;
    this.clock = clock;
    this.torrent = torrent;
    peersQueue = torrent.getPeersQueue();
    this.pieceDownloadScheduler = torrent.getPieceDownloadScheduler();
    selector = Selector.open();
    ioPollingTask = torrent.getScheduledExecutorService()
        .scheduleWithFixedDelay(this::pollConnectionsIO, 50, 50, TimeUnit.MILLISECONDS);
  }

  private void pollConnectionsIO() {
    try {
      pollIO();
      keepAliveInactiveConnections();
    } catch (Exception e) {
      log.error("Error occurred while handling I/O", e);
      torrent.shutdown();
    }
  }

  private void pollIO() throws IOException {
    selector.selectNow();
    Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
    while (keys.hasNext()) {
      SelectionKey selectionKey = keys.next();
      keys.remove();
      SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
      PeerIOState peerIOState = getPeerIOStateFromKey(selectionKey);
      Peer peer = peerIOState.getPeer();
      try {
        if (selectionKey.isReadable()) {
          peerIOState.setLastActivity();
          onDataAvailable(peer, socketChannel);
        }
        if (selectionKey.isWritable()) {
          Optional<IMessage> maybeMessage = Optional.ofNullable(
              peerIOState.getMessageQueue().poll());
          if (maybeMessage.isPresent()) {
            maybeMessage.get().send(socketChannel);
            peerIOState.setLastActivity();
          }
          Optional<IMessage> blockRequestMessage = peer.getNextPendingBlock();
          if (blockRequestMessage.isPresent()) {
            blockRequestMessage.get().send(socketChannel);
            peerIOState.setLastActivity();
          }
        }
      } catch (IOException | BitTorrentProtocolViolationException e) {
        log.error("Error occurred while reading from socket of peer {}", peer, e);
        pieceDownloadScheduler.failBlocksDownload(peer.drainInProgress(), peer);
        cancelPeerConnection(selectionKey);
      }
    }
  }

  private void keepAliveInactiveConnections() {
    for (SelectionKey key : selector.keys()) {
      PeerIOState peerIOState = (PeerIOState) key.attachment();
      if (peerIOState.getLastActivity().plusSeconds(60).isBefore(clock.instant())) {
        log.info("Connection inactive for more than 60 sec, queuing keep alive to peer {}",
            peerIOState.getPeer());
        peerIOState.queueMessage(messageFactory.buildKeepAlive());
      }
    }
  }

  @Override
  public void registerConnection(SocketChannel socketChannel, Peer peer) {
    log.debug("Registering peer {} for IO", peer);
    SelectionKey selectionKey = null;
    try {
      PeerIOState peerIOState = new PeerIOState(peer, clock);
      peerIOState.setLastActivity();
      selectionKey = socketChannel.register(selector, SelectionKey.OP_READ, peerIOState);
      selectionKey.interestOpsOr(SelectionKey.OP_WRITE);
      messageFactory.buildInterested(peer).send(socketChannel);
      peer.interested();
    } catch (IOException e) {
      log.error("Exception occurred while starting IO for Peer {}", peer, e);
      cancelPeerConnection(selectionKey);
    }
  }

  @Override
  public int getTotalActiveConnections() {
    return selector.keys().size();
  }

  private void onDataAvailable(Peer peer, SocketChannel socketChannel)
      throws IOException, BitTorrentProtocolViolationException {
    while (peer.canRead(socketChannel)) {
      IMessage message = peer.readMessage();
      message.process();
    }
  }

  private void cancelPeerConnection(SelectionKey selectionKey) {
    if (null != selectionKey) {
      Peer peer = getPeerIOStateFromKey(selectionKey).getPeer();
      log.warn("Cancelling peer connection {}", peer);
      peer.setLastActive();
      peersQueue.offer(peer);
      selectionKey.cancel();
      SelectableChannel socket = selectionKey.channel();
      try {
        socket.close();
      } catch (IOException e) {
        log.error("Error occurred while closing the socket {}!!! Fix this...", socket);
      }
    }
  }

  private PeerIOState getPeerIOStateFromKey(SelectionKey key) {
    return (PeerIOState) key.attachment();
  }

  @Override
  public void stop() {
    ioPollingTask.cancel(true);
  }
}
