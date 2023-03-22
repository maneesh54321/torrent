package com.maneesh.meta;

import java.util.Arrays;

public class Content {

  private final String rootDirectoryName;

  private final DownloadFile[] downloadFiles;

  private final int[] startIndices;

  public Content(String rootDirectoryName, DownloadFile[] downloadFiles) {
    this.rootDirectoryName = rootDirectoryName;
    this.downloadFiles = downloadFiles;
    startIndices = new int[downloadFiles.length];
    for (int i = 0; i < downloadFiles.length; i++) {
      startIndices[i] = downloadFiles[i].getPieceStartIndex();
    }
  }

  public DownloadFile getDownloadFileByPieceIndex(int pieceIndex){
    return downloadFiles[findFileIndexByPieceIndex(pieceIndex)];
  }

  private int findFileIndexByPieceIndex(int pieceIndex) {
    if(startIndices.length == 1){
      return 0;
    }
    int l = 0, r = startIndices.length - 1;
    int mid;
    while (true) {
      if (r - l == 1) {
        return l;
      }
      mid = l + (r - l) / 2;
      if (startIndices[mid] == pieceIndex) {
        return mid;
      } else if (startIndices[mid] < pieceIndex) {
        l = mid;
      } else {
        r = mid;
      }
    }
  }

  public String getRootDirectoryName() {
    return rootDirectoryName;
  }

  public DownloadFile[] getDownloadFiles() {
    return downloadFiles;
  }

  @Override
  public String toString() {
    return "Content{" +
        "rootDirectoryName='" + rootDirectoryName + '\'' +
        ", downloadFiles=" + Arrays.toString(downloadFiles) +
        '}';
  }
}
