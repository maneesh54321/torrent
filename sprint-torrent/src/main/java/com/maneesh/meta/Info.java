package com.maneesh.meta;

import com.dampcake.bencode.Bencode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

public class Info {

  private static final Logger log = LoggerFactory.getLogger(Info.class);

  private static final Bencode bencode = new Bencode(true);

  private final Long pieceLength;

  private final byte[] pieces;

  private final Long isPrivate;

  private final byte[] infoHash;

  private final Content content;

  private long totalSizeInBytes;

  private int totalPieces;

  public Info(Map<String, Object> infoMap) {
    log.info("Info Map: {}", infoMap);
    this.infoHash = calculateInfoHash(infoMap);
    this.pieceLength = (Long) infoMap.get("piece length");
    this.isPrivate = Long.valueOf(infoMap.getOrDefault("private", 0) + "");
    this.pieces = ((ByteBuffer) infoMap.get("pieces")).array();

    // Create content representation
    {
      long totalSizeInBytes = 0;
      if (infoMap.containsKey("files")) {
        // multi file mode
        var files = (List<Map<String, Object>>) infoMap.get("files");
        var rootDirectoryName = new String(((ByteBuffer) infoMap.get("name")).array(),
            StandardCharsets.UTF_8);
        var downloadFiles = new DownloadFile[files.size()];

        var pieceStartIndex = 0;
        var i = 0;
        // check if access of file in files is ordered because that will impact piece start index
        for (Map<String, Object> file : files) {
          var path = ((List<ByteBuffer>) file.get("path")).stream()
              .map(pathByteBuffer -> new String(pathByteBuffer.array()))
              .toList();
          var length = (long) file.get("length");
          totalSizeInBytes += length;
          var numberOfPieces = length / pieceLength;
          numberOfPieces = length % pieceLength == 0 ? numberOfPieces : (numberOfPieces + 1);
          totalPieces += (int) numberOfPieces;
          downloadFiles[i++] = new DownloadFile(
              path.getLast(),
              path, length, pieceStartIndex,
              numberOfPieces,
              (String) file.getOrDefault("md5Sum", "")
          );
          pieceStartIndex += (int) numberOfPieces;
        }
        content = new Content(rootDirectoryName, downloadFiles);
      } else {
        // single file mode
        var downloadFiles = new DownloadFile[1];
        var name = new String(((ByteBuffer) infoMap.get("name")).array(),
            StandardCharsets.UTF_8);
        var md5Sum = (String) infoMap.get("md5Sum");
        var length = (long) infoMap.get("length");
        var numberOfPieces = length / pieceLength;
        numberOfPieces = length % pieceLength == 0 ? numberOfPieces : (numberOfPieces + 1);
        totalPieces += (int) numberOfPieces;
        totalSizeInBytes += length;
        downloadFiles[0] = new DownloadFile(name, null, length, 0, numberOfPieces, md5Sum);
        content = new Content(null, downloadFiles);
      }

      this.totalSizeInBytes = totalSizeInBytes;
    }
  }

  private static byte[] calculateInfoHash(Map<String, Object> info) {
    try {
      var md = MessageDigest.getInstance("SHA-1");
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

  public int getTotalPieces() {
    return totalPieces;
  }

  public Content getContent() {
    return content;
  }

  @Override
  public String toString() {
    return "Info{" +
        "pieceLength=" + pieceLength +
        ", isPrivate=" + isPrivate +
        ", content=" + content +
        ", totalSizeInBytes=" + totalSizeInBytes +
        '}';
  }
}
