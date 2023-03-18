package com.maneesh;

import com.maneesh.core.LongRunningProcess;
import com.maneesh.core.Torrent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

  private static final Logger log = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
//    System.out.println((1 << 14));// 100000000000000
    Torrent torrent = null;
    try {
      torrent = new Torrent(
          "/Users/maneesh/Downloads/Adam Lambert - High Drama (2023) Mp3 320kbps.torrent");
    } catch (Exception e) {
      log.error("Shutting down the client!!");
      assert torrent != null;
      ((LongRunningProcess) torrent.getPeerIOHandler()).stop();
      ((LongRunningProcess) torrent.getConnectionHandler()).stop();
      ((LongRunningProcess) torrent.getHandshakeHandler()).stop();
      ((LongRunningProcess) torrent.getPieceDownloadScheduler()).stop();
      torrent.getPeersCollector().stop();
      torrent.getScheduledExecutorService().shutdown();
      torrent.getContentManager().shutdown();
    }
  }
}