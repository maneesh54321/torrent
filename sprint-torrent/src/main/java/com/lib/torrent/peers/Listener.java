package com.lib.torrent.peers;

public interface Listener extends Comparable<Listener> {
  void update();
}
