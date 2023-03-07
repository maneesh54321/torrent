package com.maneesh.meta;

import java.util.List;
import java.util.Optional;

public class DownloadFile implements Comparable<DownloadFile> {

  private final String name;

  private final List<String> path;

  private final int pieceStartIndex;

  private final long numOfPieces;

  private final String md5Sum;

  public DownloadFile(String name, List<String> path, int pieceStartIndex, long numOfPieces,
      String md5Sum) {
    this.name = name;
    this.path = path;
    this.pieceStartIndex = pieceStartIndex;
    this.numOfPieces = numOfPieces;
    this.md5Sum = md5Sum;
  }

  @Override
  public String toString() {
    return "DownloadFile{" +
        "name='" + name + '\'' +
        ", path=" + path +
        ", pieceStartIndex=" + pieceStartIndex +
        ", numberOfPieces=" + numOfPieces +
        ", md5Sum='" + md5Sum + '\'' +
        '}';
  }

  public List<String> getPath() {
    return path;
  }

  public String getName() {
    return name;
  }

  public int getPieceStartIndex() {
    return pieceStartIndex;
  }

  public Optional<String> getMd5Sum() {
    return Optional.ofNullable(md5Sum);
  }

  @Override
  public int compareTo(DownloadFile o) {
    if (o == null) {
      return 1;
    }
    return Integer.compare(this.pieceStartIndex, o.pieceStartIndex);
  }
}
