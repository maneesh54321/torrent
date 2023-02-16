package com.lib.torrent.parser;

import com.dampcake.bencode.Bencode;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class Info {

  private final static Bencode bencode = new Bencode(true);
  private final Long pieceLength;
  private final byte[] pieces;
  private final int isPrivate;
  private final byte[] infoHash;
  private Map<String, Object> infoMap;
  private List<DownloadFile> downloadFiles;

  private long totalSizeInBytes;

  public Info(Map<String, Object> infoMap) {
    this.infoMap = infoMap;
    System.out.println("Info Map: " + infoMap);
    this.infoHash = calculateInfoHash(this.infoMap);
    this.pieceLength = (Long) infoMap.get("piece length");
    this.isPrivate = (int) infoMap.getOrDefault("private", 0);
    this.pieces = ((ByteBuffer) infoMap.get("pieces")).array();

    // Create content representation
    if (null != pieces) {
      long totalSizeInBytes = 0;
      this.downloadFiles = new ArrayList<>();
      if (infoMap.containsKey("files")) {
        // multi file mode
        List<Map<String, Object>> files = (List<Map<String, Object>>) infoMap.get("files");

        int pieceStartIndex = 0;

        // check if access of file in files is ordered because that will impact piece start index
        for (Map<String, Object> file : files) {
          List<String> path = ((List<ByteBuffer>) file.get("path")).stream()
              .map(pathByteBuffer -> new String(pathByteBuffer.array()))
              .collect(Collectors.toList());
          long length = (long) file.get("length");
          totalSizeInBytes += length;
          long numberOfPieces = length / pieceLength;
          downloadFiles.add(
              new DownloadFile(
                  path.get(path.size() - 1),
                  path,
                  pieceStartIndex,
                  numberOfPieces,
                  (String) file.getOrDefault("md5Sum", "")
              )
          );
          pieceStartIndex += length / pieceLength - 1;
        }
      } else {
        // single file mode
        String name = new String(((ByteBuffer) infoMap.get("name")).array(),
            StandardCharsets.UTF_8);
        String md5Sum = (String) infoMap.get("md5Sum");
        long length = (long) infoMap.get("length");
        totalSizeInBytes += length;
        DownloadFile downloadFile = new DownloadFile(name, null, 0, pieces.length, md5Sum);
        this.downloadFiles.add(downloadFile);
      }

      this.totalSizeInBytes = totalSizeInBytes;
    }


  }

  private static byte[] calculateInfoHash(Map<String, Object> info) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      return md.digest(bencode.encode(info));
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public Long getPieceLength() {
    return pieceLength;
  }

  public byte[] getInfoHash() {
    return infoHash;
  }

  public byte[] getPieces() {
    return pieces;
  }

  public long getTotalSizeInBytes() {
    return totalSizeInBytes;
  }

  @Override
  public String toString() {
    return "Info{" +
        "pieceLength=" + pieceLength +
        ", pieces.length=" + Arrays.toString(pieces).length() +
        ", isPrivate=" + isPrivate +
        ", infoHash=" + Arrays.toString(infoHash) +
        ", downloadFiles=" + downloadFiles +
        '}';
  }
}
