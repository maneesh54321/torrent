package com.maneesh.content.impl;


import com.maneesh.content.ContentManager;
import com.maneesh.content.DownloadedBlock;
import java.io.IOException;

public class WriteBlockTask implements Runnable {

  private final DownloadedBlock downloadedBlock;

  private final ContentManager contentManager;

  public WriteBlockTask(DownloadedBlock downloadedBlock, ContentManager contentManager) {
    this.downloadedBlock = downloadedBlock;
    this.contentManager = contentManager;
  }

  @Override
  public void run() {
    try {
      contentManager.writeToDisk(downloadedBlock);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
