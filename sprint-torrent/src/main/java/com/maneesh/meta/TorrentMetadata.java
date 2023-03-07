package com.maneesh.meta;

import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TorrentMetadata {

  private static final Logger log = LoggerFactory.getLogger(TorrentMetadata.class);

  private final static Bencode bencode = new Bencode(true);

  private final String announce;
  private final Date creationDate;
  private final String comment;
  private final String createdBy;
  private final String encoding;
  private final Info info;
  private final List<String> announceList;

  private TorrentMetadata(String announce, List<String> announceList, Date creationDate, String comment,
      String createdBy, String encoding, Info info) {
    this.announce = announce;
    this.announceList = announceList;
    this.creationDate = creationDate;
    this.comment = comment;
    this.createdBy = createdBy;
    this.encoding = encoding;
    this.info = info;
  }

  public static TorrentMetadata parseTorrentFile(InputStream inputStream) throws IOException {
    Map<String, Object> decodedMetaInfo = bencode.decode(
        inputStream.readAllBytes(),
        Type.DICTIONARY
    );
    log.info("Decoded meta info: \n" + decodedMetaInfo);
    String announce = new String(((ByteBuffer) decodedMetaInfo.get("announce")).array(),
        StandardCharsets.UTF_8);
    String createdBy = new String(((ByteBuffer) decodedMetaInfo.getOrDefault("created by",
        ByteBuffer.allocate(0))).array(), StandardCharsets.UTF_8);
    String comment = new String(((ByteBuffer) decodedMetaInfo.getOrDefault("comment",
        ByteBuffer.allocate(0))).array(), StandardCharsets.UTF_8);
    Info info = new Info((Map<String, Object>) decodedMetaInfo.get("info"));
    Date creationDate = new Date((Long) decodedMetaInfo.getOrDefault("creation date", 0));
    String encoding = new String(((ByteBuffer) decodedMetaInfo.getOrDefault("encoding",
        ByteBuffer.allocate(0))).array(), StandardCharsets.UTF_8);
    List<String> announceList = new ArrayList<>();
    if (decodedMetaInfo.containsKey("announce-list")) {
      announceList.addAll(((List<List<ByteBuffer>>) decodedMetaInfo.get("announce-list")).stream()
          .flatMap(announceUrlList -> announceUrlList.stream()
              .map(announceByteBuffer -> new String(announceByteBuffer.array(),
                  StandardCharsets.UTF_8))
          ).toList());
    }

    TorrentMetadata torrentMetadata = new TorrentMetadata(announce, announceList, creationDate, comment, createdBy,
        encoding, info);
    log.info("MetaInfo: \n" + torrentMetadata);
    return torrentMetadata;
  }

  public String getAnnounce() {
    return announce;
  }

  public Info getInfo() {
    return info;
  }

  @Override
  public String toString() {
    return "MetaInfo{" +
        "announce='" + announce + '\'' +
        ", announceList=" + announceList +
        ", creationDate=" + creationDate +
        ", comment='" + comment + '\'' +
        ", createdBy='" + createdBy + '\'' +
        ", encoding='" + encoding + '\'' +
        ", info=" + info +
        '}';
  }

  public List<String> getAnnounceList() {
    return announceList;
  }
}
