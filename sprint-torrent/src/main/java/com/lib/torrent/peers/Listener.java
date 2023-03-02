package com.lib.torrent.peers;

import java.io.IOException;

public interface Listener extends Comparable<Listener> {
  void update();
}
