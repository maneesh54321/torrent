package com.lib.torrent.downloader;

import java.io.IOException;

public interface TCPClient {
  void startConnection();

  void stopConnection();
}
