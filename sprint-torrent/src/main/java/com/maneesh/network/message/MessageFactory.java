package com.maneesh.network.message;

import com.maneesh.network.exception.BitTorrentProtocolViolationException;
import java.nio.ByteBuffer;

public class MessageFactory {

  public static IMessage build(int length, int messageId, ByteBuffer payload)
      throws BitTorrentProtocolViolationException {
    if (length == 0) {
      return new KeepAliveMessage();
    } else {
      switch (messageId) {
        case 0:
          return new ChokeMessage();
        case 1:
          return new UnChokeMessage();
        case 2:
          return new InterestedMessage();
        case 3:
          return new NotInterestedMessage();
        case 4:
          return new HaveMessage(payload.getInt());
        case 5:
          byte[] bitfield = new byte[length-1];
          payload.get(bitfield);
          return new BitfieldMessage(bitfield);
        case 6:
          return new BlockRequestMessage();
        case 7:
          int pieceIndex = payload.getInt();
          int offset = payload.getInt();
          byte[] data = new byte[length-9];
          payload.get(data);
          return new PieceMessage(pieceIndex, offset, data);
        case 8:
          return new CancelMessage();
        case 9:
          return new PortMessage();
      }
    }
    throw new BitTorrentProtocolViolationException("Invalid message received!!!");
  }

}
