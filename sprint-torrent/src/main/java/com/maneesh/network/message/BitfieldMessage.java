package com.maneesh.network.message;

import com.maneesh.core.Peer;
import com.maneesh.core.Torrent;
import java.nio.ByteBuffer;

public class BitfieldMessage extends NioSocketMessage {

  private final boolean[] bitfield;

  private final Torrent torrent;

  private final Peer peer;

  public BitfieldMessage(byte[] bitfield, Torrent torrent, Peer peer) {
    this.bitfield = convertBitfieldBytesToBitfield(bitfield);
    this.peer = peer;
    this.torrent = torrent;
  }

  @Override
  protected ByteBuffer convertToBytes() {
    return null;
  }

  @Override
  public void process() {
    for (int i = 0; i < bitfield.length; i++) {
      if(bitfield[i]){
        torrent.getAvailablePieceStore().addAvailablePiece(i, peer);
      }
    }
  }

  private boolean[] convertBitfieldBytesToBitfield(byte[] bitfieldBytes) {

    boolean[] bitfield = new boolean[bitfieldBytes.length * 8];

    for (int i = 0; i < bitfieldBytes.length; i++) {
      for (int j = 0; j < 8; j++) {
        int bitIndex = i * 8 + j;
        bitfield[bitIndex] = ((bitfieldBytes[i] >> (7 - j)) & 1) == 1;
      }
    }

    return bitfield;
  }

  @Override
  public String toString() {
    return "BitfieldMessage{" +
        "bitfield=" + bitfield +
        '}';
  }
}
