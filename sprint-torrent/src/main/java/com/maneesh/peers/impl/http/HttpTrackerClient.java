package com.maneesh.peers.impl.http;

import com.maneesh.core.Torrent;
import com.maneesh.peers.TrackerClient;
import com.maneesh.peers.impl.TrackerResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpTrackerClient implements TrackerClient {

  private static final Logger log = LoggerFactory.getLogger(HttpTrackerClient.class);

  @Override
  public Optional<TrackerResponse> requestPeers(String url, Torrent torrent) {
    try {
      HttpTrackerRequest trackerRequest = TrackerRequestBuilder.aTrackerRequest()
          .withInfoHash(torrent.getTorrentMetadata().getInfo().getInfoHash())
          .withPeerId(torrent.getPeerId())
          .withPort(torrent.getPort())
          .withUploaded(torrent.getUploaded())
          .withDownloaded(torrent.getDownloaded())
          .withLeft(torrent.getLeft())
          .compact(0)
          .noPeerId(0)
          .build();

      HttpURLConnection connection = (HttpURLConnection) trackerRequest.getUrlEncodedUrl(url)
          .openConnection();
      connection.setRequestMethod("GET");
      connection.setRequestProperty("Connection", "close");
      connection.setRequestProperty("Accept-Encoding", "gzip");

      var dataIs = connection.getInputStream();

      byte[] bytes = dataIs.readAllBytes();

      TrackerResponseHandler trackerResponseHandler = TrackerResponseHandler
          .getInstance(Torrent.BENCODE);

      return trackerResponseHandler.extractTrackerResponse(bytes);
    } catch (IOException e) {
      log.warn("Failed to fetch peers from the tracker {}", url);
    }

    return Optional.empty();
  }
}
