package com.maneesh.peers.impl.http;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

class HttpTrackerRequest {

  private final byte[] infoHash;

  private final String peerId;

  private final int port;

  private final long uploaded;

  private final long downloaded;

  private final long left;

  private int compact;

  private int noPeerId;

  HttpTrackerRequest(byte[] infoHash, String peerId, int port, long uploaded, long downloaded,
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

  public URL getUrlEncodedUrl(String announceUrl) throws MalformedURLException {
      /*var newUri = "http://130.239.18.158:6969/announce?info_hash=%60%8e%0d%d1%b3%fa%91%ba%e7d%c
      0%dc%c9tK%d6P%a2%de%a6&peer_id=-qB4450-_TCDJdvQG(MO&port=38418&uploaded=0&downloaded=0&left=
      16384&corrupt=0&key=0AEBAC14&event=started&numwant=200&compact=0&no_peer_id=1&redundant=0";*/
    String infoHashBytesString = new String(infoHash, StandardCharsets.ISO_8859_1);
//    var uriComponents = UriComponentsBuilder.fromHttpUrl(announceUrl)
//        .queryParam(INFO_HASH, infoHashBytesString)
//        .queryParam(PEER_ID, peerId)
//        .queryParam(PORT, port)
//        .queryParam(UPLOADED, uploaded)
//        .queryParam(DOWNLOADED, downloaded)
//        .queryParam(LEFT, left)
//        .queryParam(COMPACT, 0)
//        .queryParam(EVENT, "started")
//        .queryParam(NO_PEER_ID, 0)
//        .build().encode(StandardCharsets.ISO_8859_1);

    String encodedInfoHash = URLEncoder.encode(infoHashBytesString, StandardCharsets.UTF_8);
    String encodedPeerId = URLEncoder.encode(peerId, StandardCharsets.UTF_8);
    String encodedPort = URLEncoder.encode(String.valueOf(port), StandardCharsets.UTF_8);
    String encodedUploaded = URLEncoder.encode(String.valueOf(uploaded), StandardCharsets.UTF_8);
    String encodedDownloaded = URLEncoder.encode(String.valueOf(downloaded),
        StandardCharsets.UTF_8);
    String encodedLeft = URLEncoder.encode(String.valueOf(left), StandardCharsets.UTF_8);
    String encodedCompact = URLEncoder.encode(String.valueOf(compact), StandardCharsets.UTF_8);
    String encodedEvent = URLEncoder.encode("started", StandardCharsets.UTF_8);
    String encodedNoPeerId = URLEncoder.encode(String.valueOf(noPeerId), StandardCharsets.UTF_8);

    String httpUrl = String.format(
        "%s?info_hash=%s&peer_id=%s&port=%s&uploaded=%s&downloaded=%s&left=%s&compact=%s&event=%s&no_peer_id=%s",
        announceUrl, encodedInfoHash, encodedPeerId, encodedPort, encodedUploaded,
        encodedDownloaded, encodedLeft, encodedCompact,
        encodedEvent, encodedNoPeerId);

    return new URL(httpUrl);
  }
}
