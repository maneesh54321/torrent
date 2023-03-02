package com.lib.torrent.downloader.nonblocking;

import com.lib.torrent.downloader.BlockRequest;
import com.lib.torrent.downloader.DownloadedBlock;
import com.lib.torrent.downloader.Message;
import com.lib.torrent.downloader.exception.ConnectionChokedException;
import com.lib.torrent.piece.AvailablePieceStore;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReadyToDownloadState implements ConnectionState {

  private static final Logger log = LoggerFactory.getLogger(ReadyToDownloadState.class);

  private final Connection connection;

  private final AvailablePieceStore availablePieceStore;

  public ReadyToDownloadState(Connection connection, AvailablePieceStore availablePieceStore) {
    this.connection = connection;
    this.availablePieceStore = availablePieceStore;
  }

  @Override
  public ByteBuffer getRequestMessage() {
    log.debug("Creating block request message...");
    Optional<BlockRequest> blockRequestOptional = this.connection.getDownloadingPiece()
        .getBlockToDownload(this.connection);
    if (blockRequestOptional.isPresent()) {
      BlockRequest blockRequest = blockRequestOptional.get();
      return Message.buildDownloadRequest(blockRequest.getPieceIndex(),
          blockRequest.getOffset(), blockRequest.getLength()).flip();
    }
    return null;
  }

  @Override
  public void handleResponse(SocketChannel socketChannel) {
    try {
      ByteBuffer header = ByteBuffer.allocate(5);
      socketChannel.read(header);
      header.flip();
      int length = header.getInt();
      int id = header.get();
      if (id == 4) {
        ByteBuffer payload = ByteBuffer.allocate(1);
        socketChannel.read(payload);
        payload.flip();
        // handle have message index
        this.availablePieceStore.addAvailablePiece(payload.get(), this.connection.getPeer());
      } else if (id == 7) {
        ByteBuffer pieceHeader = ByteBuffer.allocate(8);
        ByteBuffer payload = ByteBuffer.allocate(length - 9);
        socketChannel.read(pieceHeader);
        pieceHeader.flip();
        socketChannel.read(payload);
        payload.flip();
        // TODO handle piece message
        this.connection.getDownloadingPiece().completeDownload(
            new DownloadedBlock(pieceHeader.getInt(), pieceHeader.getInt(), payload.array()));
      } else if (id == 0) {
        log.debug("Connection choked by peer {}", this.connection.getPeer());
        throw new ConnectionChokedException("Connection has been choked by peer!!!");
      }
      // TODO handle other types of messages that can be received.
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
