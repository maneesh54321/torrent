package com.maneesh.peers;

import com.maneesh.core.Torrent;
import com.maneesh.peers.impl.TrackerResponse;
import java.util.Optional;

public interface TrackerClient {
  Optional<TrackerResponse> requestPeers(String url, Torrent torrent);
}
