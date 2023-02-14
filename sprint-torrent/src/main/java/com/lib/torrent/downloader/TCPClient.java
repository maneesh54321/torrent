package com.lib.torrent.downloader;

import java.io.IOException;

public interface TCPClient {
  boolean startConnection() throws IOException;

  boolean stopConnection();
}
