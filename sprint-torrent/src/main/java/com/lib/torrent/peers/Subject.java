package com.lib.torrent.peers;

public interface Subject {
  void registerListener();

  void removeListener();

  void notifyListeners();
}
