package com.maneesh.network.state;

import com.maneesh.core.Peer;
import com.maneesh.network.exception.BitTorrentProtocolViolationException;
import com.maneesh.network.message.BlockMessage;
import com.maneesh.network.message.IMessage;
import com.maneesh.network.message.MessageFactory;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Queue;

public class PeerConnectionState {

  private static final int LENGTH_BUFFER_SIZE = 4;
  private static final int BUFFER_SIZE = (1 << 14) + LENGTH_BUFFER_SIZE;
  private final Queue<BlockMessage> blocksQueue;
  private final Peer peer;
  private ByteBuffer buffer;

  public PeerConnectionState(Peer peer) {
    this.peer = peer;
    this.blocksQueue = new ArrayDeque<>();
    buffer = ByteBuffer.allocate(BUFFER_SIZE);
    buffer.limit(LENGTH_BUFFER_SIZE);
  }

  public boolean canRead(SocketChannel socket)
      throws IOException {
    socket.read(buffer);
    if (buffer.position() >= LENGTH_BUFFER_SIZE) {
      ByteBuffer readOnlyBuffer = buffer.asReadOnlyBuffer();
      int length = readOnlyBuffer.flip().getInt();
      int bytesNeeded = length + LENGTH_BUFFER_SIZE;
      if (buffer.capacity() < bytesNeeded) {
        growBuffer(bytesNeeded);
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
    buffer = ByteBuffer.wrap(newBuffer);
  }

  public IMessage readMessage() throws BitTorrentProtocolViolationException {
    // read message from buffer
    int length = buffer.getInt();
    int messageId = buffer.get();
    IMessage message = MessageFactory.build(length, messageId, buffer);
    // reset the buffer for next read.
    resetBuffer();
    return message;
  }

  private void resetBuffer() {
    if(buffer.limit() > BUFFER_SIZE){
      buffer = ByteBuffer.allocate(BUFFER_SIZE);
    }
    buffer.position(0);
    buffer.limit(LENGTH_BUFFER_SIZE);
  }

  public Queue<BlockMessage> getBlocksQueue() {
    return blocksQueue;
  }

  public Peer getPeer() {
    return peer;
  }
}
