package com.lib.torrent.downloader;

public class PeerDownloadTask implements Runnable {

  private TCPClient tcpClient;

  public PeerDownloadTask(TCPClient tcpClient) {
    this.tcpClient = tcpClient;
  }

  @Override
  public void run() {
    tcpClient.startConnection();
  }
}
