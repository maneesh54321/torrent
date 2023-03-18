package com.maneesh.network.message;

import com.maneesh.core.Peer;
import com.maneesh.core.Torrent;
import com.maneesh.network.exception.BitTorrentProtocolViolationException;
import java.nio.ByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageFactory {

  private static final Logger log = LoggerFactory.getLogger(MessageFactory.class);

  private final Torrent torrent;

  public MessageFactory(Torrent torrent) {
    this.torrent = torrent;
  }

  public IMessage build(int size, int messageId, ByteBuffer payload, Peer peer)
      throws BitTorrentProtocolViolationException {
    if (size == 0) {
      return new KeepAliveMessage();
    } else {
      switch (messageId) {
        case 0 -> {
          log.debug("Choke Message received!!");
          return new ChokeMessage(peer);
        }
        case 1 -> {
          log.debug("UnChoke message received!!");
          return new UnChokeMessage(peer);
        }
        case 2 -> {
          log.debug("Interested message received!!");
          return new InterestedMessage(peer);
        }
        case 3 -> {
          log.debug("Not Interested message received!!");
          return new NotInterestedMessage(peer);
        }
        case 4 -> {
          log.debug("Have message received!!");
          return new HaveMessage(payload.getInt(), torrent, peer);
        }
        case 5 -> {
          log.debug("Bitfield message received!!");
          byte[] bitfield = new byte[size - 1];
          payload.get(bitfield);
          return new BitfieldMessage(bitfield, torrent, peer);
        }
        case 6 -> {
          log.debug("Block request message received!!");
          return new BlockRequestMessage(payload.getInt(), payload.getInt(), payload.getInt());
        }
        case 7 -> {
          log.debug("Piece message received!!");
          int pieceIndex = payload.getInt();
          int offset = payload.getInt();
          byte[] data = new byte[size - 9];
          payload.get(data);
          return new PieceMessage(torrent, pieceIndex, offset, data);
        }
        case 8 -> {
          log.debug("Cancel message received!!");
          int pieceIndex = payload.getInt();
          int offset = payload.getInt();
          int length = payload.getInt();
          return new CancelMessage(pieceIndex, offset, length, peer);
        }
        case 9 -> {
          log.debug("Port message received!!");
          return new PortMessage(peer);
        }
      }
    }
    throw new BitTorrentProtocolViolationException("Invalid message received!!!");
  }

  public IMessage buildInterested(Peer peer) {
    return new InterestedMessage(peer);
  }

}
