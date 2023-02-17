package com.lib.torrent.downloader;

public enum MessageType {
  CHOKE(0), UNCHOKE(1), HAVE(4), BITFIELD(5), PIECE(7), PIECE_REQUEST(6),
  INTERESTED(2), NOT_INTERESTED(3), CANCEL(8), UNKNOWN(-1);

  private int value;

  MessageType(int value) {
    this.value = value;
  }

  public static MessageType valueOf(int value){
    switch (value) {
      case 0:
        return CHOKE;
      case 1:
        return UNCHOKE;
      case 2:
        return INTERESTED;
      case 3:
        return NOT_INTERESTED;
      case 4:
        return HAVE;
      case 5:
        return BITFIELD;
      case 6:
        return PIECE_REQUEST;
      case 7:
        return PIECE;
      case 8:
        return CANCEL;
    }
    return UNKNOWN;
  }
}
