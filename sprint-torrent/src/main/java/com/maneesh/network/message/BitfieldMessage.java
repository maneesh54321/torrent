package com.maneesh.network.message;

import java.nio.channels.SocketChannel;
import java.util.BitSet;

public class BitfieldMessage implements IMessage {

  private BitSet bitfield;

  public BitfieldMessage(byte[] bitfield) {
    this.bitfield = BitSet.valueOf(bitfield);
  }

  @Override
  public void send(SocketChannel sc) {

  }

  @Override
  public void process() {

  }
}
