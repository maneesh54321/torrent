package com.maneesh.meta;

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
