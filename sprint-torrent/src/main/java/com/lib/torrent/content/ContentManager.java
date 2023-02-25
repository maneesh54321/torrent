package com.lib.torrent.content;

import com.lib.torrent.downloader.DownloadedBlock;
import java.io.IOException;

public interface ContentManager {
  void writeToDisk(DownloadedBlock downloadedBlock) throws IOException;
  void shutdown();
}
