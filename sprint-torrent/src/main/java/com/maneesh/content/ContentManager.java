package com.maneesh.content;

import java.io.IOException;

public interface ContentManager {

  /**
   * Writes the downloadedBlock to disk.
   * @param downloadedBlock
   * @throws IOException
   */
  void writeToDisk(DownloadedBlock downloadedBlock) throws IOException;

  /**
   * Writes the downloaded block to disk asynchronously
   * @param downloadedBlock
   */
  void writeToDiskAsync(DownloadedBlock downloadedBlock);

  /**
   * Shuts down this content handler gracefully.
   */
  void shutdown();
}
