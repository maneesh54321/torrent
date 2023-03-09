package com.maneesh.network.impl;

import com.maneesh.core.LongRunningProcess;
import com.maneesh.core.Peer;
import com.maneesh.core.Torrent;
import com.maneesh.network.HandshakeHandler;
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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NioHandshakeHandler implements HandshakeHandler, LongRunningProcess {

  private static final Logger log = LoggerFactory.getLogger(NioHandshakeHandler.class);
  private static final int HANDSHAKE_MSG_LEN = 68;
  private static final String BITTORRENT_PROTOCOL_NAME = "Bittorrent Protocol";
  private static final int HANDSHAKE_TIMEOUT_SECONDS = 5;
  private final Selector selector;
  private final Clock clock;
  private final Torrent torrent;
  private final ScheduledFuture<?> pollConnections;
  private final ByteBuffer handshakeMessage;

  public NioHandshakeHandler(Clock clock, Torrent torrent) {
    try {
      selector = Selector.open();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    this.clock = clock;
    this.torrent = torrent;
    this.handshakeMessage = ByteBuffer.allocate(HANDSHAKE_MSG_LEN);
    this.handshakeMessage.put((byte) 0x13);
    putProtocolString(this.handshakeMessage);
    this.handshakeMessage.putInt(0);
    this.handshakeMessage.position(this.handshakeMessage.position() + 20);
    this.handshakeMessage.put(torrent.getPeerId().getBytes());

    this.pollConnections = torrent.getScheduledExecutorService().scheduleAtFixedRate(this::pollConnectedPeers, 50, 50,
        TimeUnit.MILLISECONDS);
  }

  private void putProtocolString(ByteBuffer buffer) {
    for (int i = 0; i < BITTORRENT_PROTOCOL_NAME.length(); i++) {
      buffer.put((byte) BITTORRENT_PROTOCOL_NAME.charAt(i));
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
              validateHandshake(messageBuffer.flip(), torrent.getTorrentMetadata().getInfo()
                  .getInfoHash());
              // promote connection to start downloading/uploading
              onHandshakeReceived(socket, handshakeState);
            } else {
              if (Duration.between(handshakeState.getHandshakeStartedAt(), clock.instant()).toSeconds() >= HANDSHAKE_TIMEOUT_SECONDS) {
                throw new HandshakeTimeoutException("Did not receive handshake within" + HANDSHAKE_TIMEOUT_SECONDS);
              }
            }
          }
          if (selectionKey.isWritable()) {
            // initiate handshake
            sendHandshake(socket);
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
  public void initiateHandshake(SocketChannel socketChannel, Peer peer) {
    try {
      socketChannel.register(selector, SelectionKey.OP_WRITE, new HandshakeState(peer))
          .interestOpsAnd(SelectionKey.OP_READ);
    } catch (ClosedChannelException e) {
      throw new RuntimeException(e);
    }
  }

  private void onHandshakeReceived(SocketChannel socketChannel, HandshakeState handshakeState) {
    // TODO register peer for IO
  }

  public void stop() {
    pollConnections.cancel(true);
  }
}
