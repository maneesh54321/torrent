package com.maneesh.network.message;

import com.maneesh.core.Peer;
import com.maneesh.core.Torrent;
import java.nio.ByteBuffer;
import java.util.BitSet;

public class BitfieldMessage extends NioSocketMessage {

  private final BitSet bitfield;

  private final Torrent torrent;

  private final Peer peer;

  public BitfieldMessage(byte[] bitfield, Torrent torrent, Peer peer) {
    this.bitfield = BitSet.valueOf(bitfield);
    this.peer = peer;
    this.torrent = torrent;
  }

  @Override
  protected ByteBuffer convertToBytes() {
    return null;
  }

  @Override
  public void process() {
    for (int i = 0; i < bitfield.length(); i++) {
      if(bitfield.get(i)){
        torrent.getAvailablePieceStore().addAvailablePiece(i, peer);
      }
    }
  }

  @Override
  public String toString() {
    return "BitfieldMessage{" +
        "bitfield=" + bitfield +
        '}';
  }
}
