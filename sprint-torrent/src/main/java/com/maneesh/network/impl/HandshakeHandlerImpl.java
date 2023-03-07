package com.maneesh.network.impl;

import com.maneesh.core.LongRunningProcess;
import com.maneesh.network.HandshakeHandler;
import com.maneesh.network.PeerConnection;
import com.maneesh.network.exception.BitTorrentProtocolViolationException;
import com.maneesh.network.exception.HandshakeTimeoutException;
import com.maneesh.network.state.HandshakeState;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandshakeHandlerImpl implements HandshakeHandler, LongRunningProcess {

  private static final Logger log = LoggerFactory.getLogger(HandshakeHandlerImpl.class);
  private static final int HANDSHAKE_MSG_LEN = 68;
  private static final String BITTORRENT_PROTOCOL_NAME = "Bittorrent Protocol";
  private static final int HANDSHAKE_TIMEOUT_SECONDS = 5;
  private final Selector selector;
  private final Clock clock;
  private final ScheduledFuture<?> pollConnections;
  private final ByteBuffer handshakeMessage;

  public HandshakeHandlerImpl(ScheduledExecutorService executorService, Clock clock,
      byte[] peerId) {
    try {
      selector = Selector.open();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    this.clock = clock;
    this.handshakeMessage = ByteBuffer.allocate(HANDSHAKE_MSG_LEN);
    this.handshakeMessage.put((byte) 0x13);
    putProtocolString(this.handshakeMessage, BITTORRENT_PROTOCOL_NAME);
    this.handshakeMessage.putInt(0);
    this.handshakeMessage.position(this.handshakeMessage.position() + 20);
    this.handshakeMessage.put(peerId);

    this.pollConnections = executorService.scheduleAtFixedRate(this::pollConnectedPeers, 50, 50,
        TimeUnit.MILLISECONDS);
  }

  private void putProtocolString(ByteBuffer buffer, String s) {
    for (int i = 0; i < s.length(); i++) {
      buffer.put((byte) s.charAt(i));
    }
  }

  private void pollConnectedPeers() {
    try {
      selector.select(selectionKey -> {
        SocketChannel socket = (SocketChannel) selectionKey.channel();
        try {
          HandshakeState handshakeState = (HandshakeState) selectionKey.attachment();
          if (selectionKey.isReadable()) {
            // read the message from socket and verify if it is a handshake.
            ByteBuffer messageBuffer = handshakeState.getMessageBuffer();
            socket.read(messageBuffer);
            if (!messageBuffer.hasRemaining()) {
              // validate message if it is handshake
              validateHandshake(messageBuffer.flip(), handshakeState.getPeerConnection().getMetaInfo().getInfo().getInfoHash());
              // promote connection to start downloading/uploading
              onHandshakeReceived(handshakeState);
            } else {
              if (Duration.between(handshakeState.getHandshakeStartedAt(), clock.instant()).toSeconds() >= HANDSHAKE_TIMEOUT_SECONDS) {
                throw new HandshakeTimeoutException("Did not receive handshake within" + HANDSHAKE_TIMEOUT_SECONDS);
              }
            }
          }
          if (selectionKey.isWritable()) {
            // initiate handshake
            sendHandshake(selectionKey, socket);
            handshakeState.setHandshakeStartedAt(clock.instant());
          }
        } catch (BitTorrentProtocolViolationException | HandshakeTimeoutException | IOException e) {
          log.warn("Handshake failed!!", e);
          // cancel the registration
          selectionKey.cancel();
          try {
            // close the socket
            socket.close();
          } catch (IOException ex) {
            log.error("Failed to close the socket!!! Fix this...");
          }
        }
      }, 3000);
    } catch (IOException e) {
      throw new RuntimeException(e);
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

  private void sendHandshake(SelectionKey selectionKey, SocketChannel socket) throws IOException {
    PeerConnection peerConnection = (PeerConnection) selectionKey.attachment();
    prepareHandshakeMessage(peerConnection.getMetaInfo().getInfo().getInfoHash());
    socket.write(handshakeMessage);
  }

  private void prepareHandshakeMessage(byte[] infoHash) {
    // add the infoHash to handshake
    handshakeMessage.position(28);
    handshakeMessage.put(infoHash);
    handshakeMessage.position(0);
  }

  @Override
  public void initiateHandshake(SocketChannel socketChannel, PeerConnection peerConnection) {
    try {
      socketChannel.register(selector, SelectionKey.OP_WRITE, new HandshakeState(peerConnection))
          .interestOpsAnd(SelectionKey.OP_READ);
    } catch (ClosedChannelException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void onHandshakeReceived(HandshakeState handshakeState) {
    // TODO register peer for IO
  }

  public void stop() {
    pollConnections.cancel(true);
  }
}
