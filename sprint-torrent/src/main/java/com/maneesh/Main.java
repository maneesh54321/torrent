package com.maneesh;

import com.maneesh.core.Torrent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

  private static final Logger log = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {

//    System.out.println(Math.ceil(10/3));
    Torrent torrent = null;
    try {
//      torrent = new Torrent("/Users/maneesh/Downloads/Adam Lambert - High Drama (2023) Mp3 320kbps.torrent");
//      torrent = new Torrent("/Users/maneesh/Work/torrent/sprint-torrent/src/main/resources/Home Wi-Fi Tuneup - Practical Steps You Can Take to Speed Up, Stabilize, and Secure Your Home Wi-Fi.torrent");
//      torrent = new Torrent("/Users/maneesh/Work/torrent/sprint-torrent/src/main/resources/The Sausage-Making Cookbook - Complete Instructions and Recipes for Making 230 Kinds.torrent");
      torrent = new Torrent("/Users/maneesh/Work/torrent/sprint-torrent/src/main/resources/Learn Python In A Week And Master It.torrent");
//      torrent = new Torrent("/Users/maneesh/Work/torrent/sprint-torrent/src/main/resources/Getting Rich Your Own Way_ Achieve All Your Financial Goals Faster Than You Ever Thought Possible by Brian Tracy EPUB.torrent");
//      torrent = new Torrent("/Users/maneesh/Work/torrent/sprint-torrent/src/main/resources/The Resume and Cover Letter Phrase Book - What to Write to Get the Job That's Right.torrent");
//      torrent = new Torrent("/Users/maneesh/Work/torrent/sprint-torrent/src/main/resources/101 Most Popular Excel Formulas.torrent");
    } catch (Exception e) {
      log.info("Error occurred while creating torrent client", e);
    } finally {
      if (torrent != null) {
        torrent.shutdown();
      }
    }
  }
}