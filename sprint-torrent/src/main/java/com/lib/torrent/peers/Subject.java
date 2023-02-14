package com.lib.torrent.peers;

public interface Subject {
  void registerListener(Listener listener);

  void removeListener(Listener listener);

  void notifyListeners();
}
