package com.maneesh;

import com.maneesh.core.Torrent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

  private static final Logger log = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
    Torrent torrent = null;
    try {
      torrent = new Torrent("/Users/maneesh/Downloads/Adam Lambert - High Drama (2023) Mp3 320kbps.torrent");
//      torrent = new Torrent("/Users/maneesh/Work/torrent/sprint-torrent/src/main/resources/Learn Python In A Week And Master It.torrent");
//      torrent = new Torrent("/Users/maneesh/Work/torrent/sprint-torrent/src/main/resources/How Data Happened_ A History from the Age of Reason to the Age of Algorithms by Chris Wiggins EPUB.torrent");
    } catch (Exception e) {
      log.info("Error occurred while creating torrent client", e);
      torrent.shutdown();
    }
  }
}