package com.lib.torrent.content;

import com.lib.torrent.parser.DownloadFile;
import java.util.Arrays;

public record Content(String rootDirectoryName, DownloadFile[] downloadFiles) {

  @Override
  public String toString() {
    return "Content{" +
        "rootDirectoryName='" + rootDirectoryName + '\'' +
        ", downloadFiles=" + Arrays.toString(downloadFiles) +
        '}';
  }
}
