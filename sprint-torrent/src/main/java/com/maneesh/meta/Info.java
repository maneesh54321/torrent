package com.maneesh.meta;

import com.dampcake.bencode.Bencode;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Info {

  private static final Logger log = LoggerFactory.getLogger(Info.class);

  private final static Bencode bencode = new Bencode(true);

  private final Long pieceLength;

  private final byte[] pieces;

  private final Long isPrivate;

  private final byte[] infoHash;

  private Content content;

  private long totalSizeInBytes;

  public Info(Map<String, Object> infoMap) {
    log.info("Info Map: " + infoMap);
    this.infoHash = calculateInfoHash(infoMap);
    this.pieceLength = (Long) infoMap.get("piece length");
    this.isPrivate = (Long) infoMap.getOrDefault("private", 0);
    this.pieces = ((ByteBuffer) infoMap.get("pieces")).array();

    // Create content representation
    if (null != pieces) {
      long totalSizeInBytes = 0;
      if (infoMap.containsKey("files")) {
        // multi file mode
        List<Map<String, Object>> files = (List<Map<String, Object>>) infoMap.get("files");
        String rootDirectoryName = new String(((ByteBuffer) infoMap.get("name")).array(),
            StandardCharsets.UTF_8);
        DownloadFile[] downloadFiles = new DownloadFile[files.size()];
        content = new Content(rootDirectoryName, downloadFiles);

        int pieceStartIndex = 0;
        int i = 0;
        // check if access of file in files is ordered because that will impact piece start index
        for (Map<String, Object> file : files) {
          List<String> path = ((List<ByteBuffer>) file.get("path")).stream()
              .map(pathByteBuffer -> new String(pathByteBuffer.array()))
              .collect(Collectors.toList());
          long length = (long) file.get("length");
          totalSizeInBytes += length;
          long numberOfPieces = length / pieceLength;
          downloadFiles[i++] = new DownloadFile(
              path.get(path.size() - 1),
              path,
              pieceStartIndex,
              numberOfPieces,
              (String) file.getOrDefault("md5Sum", "")
          );
          pieceStartIndex += length / pieceLength - 1;
        }
      } else {
        // single file mode
        DownloadFile[] downloadFiles = new DownloadFile[1];
        String name = new String(((ByteBuffer) infoMap.get("name")).array(),
            StandardCharsets.UTF_8);
        String md5Sum = (String) infoMap.get("md5Sum");
        long length = (long) infoMap.get("length");
        totalSizeInBytes += length;
        downloadFiles[0] =  new DownloadFile(name, null, 0, pieces.length, md5Sum);
        content = new Content(null, downloadFiles);
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

  public int getTotalPieces() {
    return this.pieces.length / 20;
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
