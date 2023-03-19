package com.maneesh.network.impl;

import com.maneesh.core.LongRunningProcess;
import com.maneesh.core.Peer;
import com.maneesh.core.Torrent;
import com.maneesh.network.HandshakeHandler;
import com.maneesh.network.exception.BitTorrentProtocolViolationException;
import com.maneesh.network.state.HandshakeState;
import com.maneesh.peers.PeersQueue;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.Iterator;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NioHandshakeHandler implements HandshakeHandler, LongRunningProcess {

  private static final Logger log = LoggerFactory.getLogger(NioHandshakeHandler.class);
  private static final int HANDSHAKE_MSG_LEN = 68;
  private static final String BITTORRENT_PROTOCOL_NAME = "BitTorrent protocol";
  private static final int HANDSHAKE_TIMEOUT_SECONDS = 5;
  private final Selector selector;
  private final Clock clock;
  private final Torrent torrent;
  private final ScheduledFuture<?> pollConnections;
  private final ByteBuffer handshakeMessage;

  private final PeersQueue peersQueue;

  public NioHandshakeHandler(Torrent torrent, Clock clock, PeersQueue peersQueue) {
    try {
      selector = Selector.open();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    this.clock = clock;
    this.torrent = torrent;
    this.peersQueue = peersQueue;
    this.handshakeMessage = ByteBuffer.allocate(HANDSHAKE_MSG_LEN);
    this.handshakeMessage.put((byte) 0x13);
    this.handshakeMessage.put(BITTORRENT_PROTOCOL_NAME.getBytes());
    this.handshakeMessage.putInt(0);
    this.handshakeMessage.position(this.handshakeMessage.position() + 20);
    this.handshakeMessage.put(torrent.getPeerId().getBytes());

    this.pollConnections = torrent.getScheduledExecutorService()
        .scheduleAtFixedRate(this::pollConnectedPeers, 50, 50,
            TimeUnit.MILLISECONDS);
  }

  private void pollConnectedPeers() {
    try {
      // check for handshake
      checkHandshake();
      // cancel handshake timed out connections
      cancelTimedOutConnections();
    } catch (Exception e) {
      log.error("Error occurred while polling for handshake!!!", e);
      torrent.shutdown();
    }
  }

  private void cancelTimedOutConnections() {
    for (SelectionKey selectionKey : selector.keys()) {
      HandshakeState handshakeState = getHandshakeStateFromKey(selectionKey);
      if (Duration.between(clock.instant().minusSeconds(HANDSHAKE_TIMEOUT_SECONDS),
          handshakeState.getHandshakeStartedAt()).isNegative()) {
        log.warn(
            "Did not receive handshake from peer {} within {} seconds, dropping the connection",
            handshakeState.getPeer(), HANDSHAKE_TIMEOUT_SECONDS);
        cancelPeerConnection(selectionKey);
      }
    }
  }

  private void cancelPeerConnection(SelectionKey selectionKey) {
    if (null != selectionKey) {
      Peer peer = getHandshakeStateFromKey(selectionKey).getPeer();
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

  private HandshakeState getHandshakeStateFromKey(SelectionKey key) {
    return (HandshakeState) key.attachment();
  }

  private void checkHandshake() throws IOException {
    selector.selectNow();
    Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
    while (keys.hasNext()) {
      SelectionKey selectionKey = keys.next();
      keys.remove();
      SocketChannel socket = (SocketChannel) selectionKey.channel();
      HandshakeState handshakeState = getHandshakeStateFromKey(selectionKey);
      try {
        // read the message from socket and verify if it is a handshake.
        ByteBuffer messageBuffer = handshakeState.getMessageBuffer();
        socket.read(messageBuffer);
        if (!messageBuffer.hasRemaining()) {
          // validate message if it is handshake
          validateHandshake(messageBuffer.flip(), torrent.getTorrentMetadata().getInfo()
              .getInfoHash());
          // promote connection to start downloading/uploading
          onHandshakeReceived(socket, handshakeState);
          selectionKey.cancel();
        }
      } catch (Exception e) {
        log.warn("Handshake failed with peer {}!!", handshakeState.getPeer(), e);
        // cancel the registration
        cancelPeerConnection(selectionKey);
        try {
          // close the socket
          socket.close();
        } catch (IOException ex) {
          log.error("Failed to close the socket!!! Fix this...");
        }
      }
    }
  }

  private void validateHandshake(ByteBuffer messageBuffer, byte[] infoHash)
      throws BitTorrentProtocolViolationException {
    // verify handshake
    if (messageBuffer.get() == 19) {
      byte[] protocolBuffer = new byte[19];
      messageBuffer.get(protocolBuffer);
      if (new String(protocolBuffer, StandardCharsets.UTF_8).equals(BITTORRENT_PROTOCOL_NAME)) {
        messageBuffer.position(messageBuffer.position() + 8);
        byte[] infoHashBuffer = new byte[20];
        messageBuffer.get(infoHashBuffer);
        if (MessageDigest.isEqual(infoHashBuffer, infoHash)) {
          log.debug("Completed handshake!!!");
          return;
        }
      }
    }
    throw new BitTorrentProtocolViolationException("Invalid handshake received!!");
  }

  private void sendHandshake(SocketChannel socket) throws IOException {
    prepareHandshakeMessage(torrent.getTorrentMetadata().getInfo().getInfoHash());
    socket.write(handshakeMessage);
  }

  private void prepareHandshakeMessage(byte[] infoHash) {
    // add the infoHash to handshake
    handshakeMessage.position(28);
    handshakeMessage.put(infoHash);
    handshakeMessage.position(0);
  }

  @Override
  public void onConnectionEstablished(SocketChannel socketChannel, Peer peer) {
    HandshakeState handshakeState = new HandshakeState(peer);
    handshakeState.setHandshakeStartedAt(clock.instant());
    SelectionKey selectionKey = null;
    try {
      selectionKey = socketChannel.register(selector, SelectionKey.OP_READ, handshakeState);
      log.debug("Initiating handshake with peer {}", peer);
      sendHandshake(socketChannel);
    } catch (IOException e) {
      log.warn("Exception occurred while starting handshake process for peer {}", peer, e);
      cancelPeerConnection(selectionKey);
    }
  }

  private void onHandshakeReceived(SocketChannel socketChannel, HandshakeState handshakeState)
      throws Exception {
    log.debug("Handshake received from Peer {}", handshakeState.getPeer());
    // register peer for IO
    torrent.getPeerIOHandler().registerConnection(socketChannel, handshakeState.getPeer());
  }

  public void stop() {
    pollConnections.cancel(true);
  }
}
