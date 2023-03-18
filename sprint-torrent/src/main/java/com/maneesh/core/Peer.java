package com.maneesh.core;

import com.maneesh.network.exception.BitTorrentProtocolViolationException;
import com.maneesh.network.message.IMessage;
import com.maneesh.network.message.MessageFactory;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;

public class Peer {

  private static final int LENGTH_BUFFER_SIZE = 4;

  private static final int BUFFER_SIZE = (1 << 14) + LENGTH_BUFFER_SIZE;

  private final SocketAddress socketAddress;

  private final Queue<IMessage> blockMessageQueue;
  private final MessageFactory messageFactory;
  private ByteBuffer buffer;
  private boolean choked;
  private boolean am_interested;

  public Peer(SocketAddress socketAddress, MessageFactory messageFactory) {
    this.socketAddress = socketAddress;
    blockMessageQueue = new ArrayDeque<>();
    buffer = ByteBuffer.allocate(BUFFER_SIZE);
    buffer.limit(LENGTH_BUFFER_SIZE);
    this.messageFactory = messageFactory;
    choked = true;
    am_interested = false;
  }

  public boolean canRead(SocketChannel socket) throws IOException {
    socket.read(buffer);
    if (buffer.position() >= LENGTH_BUFFER_SIZE) {
      ByteBuffer readOnlyBuffer = buffer.asReadOnlyBuffer();
      int length = readOnlyBuffer.flip().getInt();
      int bytesNeeded = length + LENGTH_BUFFER_SIZE;
      if (buffer.capacity() < bytesNeeded) {
        growBuffer(bytesNeeded);
        // since the capacity might not have been enough to read all data earlier, reading again
        socket.read(buffer);
      } else {
        buffer.limit(bytesNeeded);
      }
      return !buffer.hasRemaining();
    }
    return false;
  }

  private void growBuffer(int desiredSize) {
    // create new buffer with size = length + 4
    byte[] newBuffer = new byte[desiredSize];
    // transfer bytes from buffer to new buffer
    System.arraycopy(buffer.array(), 0, newBuffer, 0, buffer.position() + 1);
    int oldPosition = buffer.position();
    buffer = ByteBuffer.wrap(newBuffer);
    // set position to position which was there before growing the buffer.
    buffer.position(oldPosition);
  }

  public IMessage readMessage() throws BitTorrentProtocolViolationException {
    // read message from buffer
    buffer.flip();
    int length = buffer.getInt();
    IMessage message;
    if (length == 0) {
      message = messageFactory.build(length, -1, buffer, this);
    } else {
      int messageId = buffer.get();
      message = messageFactory.build(length, messageId, buffer, this);
    }
    // reset the buffer for next read.
    resetBuffer();
    return message;
  }

  private void resetBuffer() {
    if (buffer.limit() > BUFFER_SIZE) {
      buffer = ByteBuffer.allocate(BUFFER_SIZE);
    }
    buffer.position(0);
    buffer.limit(LENGTH_BUFFER_SIZE);
  }

  public SocketAddress getSocketAddress() {
    return socketAddress;
  }

  public void addMessage(IMessage message) {
    blockMessageQueue.add(message);
  }

  public Queue<IMessage> getBlockMessageQueue() {
    return blockMessageQueue;
  }

  public void interested() {
    am_interested = true;
  }

  public void notInterested() {
    am_interested = false;
  }

  public void choke() {
    choked = true;
  }

  public void unChoke() {
    interested();
    choked = false;
  }

  public boolean canDownload() {
    return am_interested && choked;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Peer peer = (Peer) o;
    return Objects.equals(socketAddress, peer.socketAddress);
  }

  @Override
  public int hashCode() {
    return Objects.hash(socketAddress);
  }

  @Override
  public String toString() {
    return "Peer{" +
        "socketAddress=" + socketAddress +
        '}';
  }
}
