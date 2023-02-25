package com.lib.torrent.peers;

import java.nio.charset.StandardCharsets;
import org.springframework.web.util.UriComponentsBuilder;

class TrackerRequest {

  public final static String INFO_HASH = "info_hash";
  public final static String PEER_ID = "peer_id";
  public final static String PORT = "port";
  public final static String EVENT = "event";
  public final static String UPLOADED = "uploaded";
  public final static String DOWNLOADED = "downloaded";
  public final static String LEFT = "left";
  public final static String NO_PEER_ID = "no_peer_id";
  public final static String COMPACT = "compact";

  private final byte[] infoHash;

  private final String peerId;

  private final int port;

  private final long uploaded;

  private final long downloaded;

  private final long left;

  private int compact;

  private int noPeerId;

  TrackerRequest(byte[] infoHash, String peerId, int port, long uploaded, long downloaded,
      long left) {
    this.infoHash = infoHash;
    this.peerId = peerId;
    this.port = port;
    this.uploaded = uploaded;
    this.downloaded = downloaded;
    this.left = left;
  }

  public void setCompact(int compact) {
    this.compact = compact;
  }

  public void setNoPeerId(int noPeerId) {
    this.noPeerId = noPeerId;
  }

  public String getUrlEncodedUrl(String announceUrl) {
      /*var newUri = "http://130.239.18.158:6969/announce?info_hash=%60%8e%0d%d1%b3%fa%91%ba%e7d%c
      0%dc%c9tK%d6P%a2%de%a6&peer_id=-qB4450-_TCDJdvQG(MO&port=38418&uploaded=0&downloaded=0&left=
      16384&corrupt=0&key=0AEBAC14&event=started&numwant=200&compact=0&no_peer_id=1&redundant=0";*/
    String infoHashBytesString = new String(infoHash, StandardCharsets.ISO_8859_1);
    var uriComponents = UriComponentsBuilder.fromHttpUrl(announceUrl)
        .queryParam(INFO_HASH, infoHashBytesString)
        .queryParam(PEER_ID, peerId)
        .queryParam(PORT, port)
        .queryParam(UPLOADED, uploaded)
        .queryParam(DOWNLOADED, downloaded)
        .queryParam(LEFT, left)
        .queryParam(COMPACT, 0)
        .queryParam(EVENT, "started")
        .queryParam(NO_PEER_ID, 0)
        .build().encode(StandardCharsets.ISO_8859_1);

    return uriComponents.toUriString();
  }
}
