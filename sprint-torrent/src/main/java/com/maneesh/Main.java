package com.maneesh;

import com.maneesh.core.Torrent;

public class Main {

  public static void main(String[] args) {
    Torrent torrent = null;
    try {
      torrent = new Torrent(
          "/Users/maneesh/Downloads/Adam Lambert - High Drama (2023) Mp3 320kbps.torrent");
    } catch (Exception e) {
        torrent.shutdown();
    }
  }
}