package com.maneesh.peers.impl;

import com.maneesh.core.LongRunningProcess;
import com.maneesh.core.Torrent;
import com.maneesh.peers.PeersStore;
import com.maneesh.peers.TrackerClient;
import com.maneesh.peers.impl.http.HttpTrackerClient;
import com.maneesh.peers.impl.udp.UDPTrackerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class TorrentPeersCollector implements LongRunningProcess {

  private static final Logger log = LoggerFactory.getLogger(TorrentPeersCollector.class);

  private final TrackerClient httpTrackerClient;

  private final TrackerClient udpTrackerClient;

  private final PeersStore peersStore;

  private final Torrent torrent;

  private ScheduledFuture<?> collectionTaskFuture;

  private final PeersCollectionTask peersCollectionTask;

  private static final Predicate<String> isHttpTracker = url -> url.startsWith("http");
  private static final Predicate<String> isUDPTracker = url -> url.startsWith("udp");

  public TorrentPeersCollector(Torrent torrent) {
    this.torrent = torrent;
    this.peersStore = (PeersStore) torrent.getPeersQueue();
    this.httpTrackerClient = new HttpTrackerClient();
    this.udpTrackerClient = new UDPTrackerClient();
    peersCollectionTask = new PeersCollectionTask(this);
    ScheduledExecutorService executorService = torrent.getScheduledExecutorService();
    collectionTaskFuture = executorService.schedule(peersCollectionTask, 50, TimeUnit.MILLISECONDS);
  }

  private void collectPeers() {
    Optional<TrackerResponse> maybeTrackerResponse = Optional.empty();
    try {
      // collect peers
      var announceList = torrent.getTorrentMetadata().getAnnounceList();
      for (var announceUrl : announceList) {
        log.info("Trying tracker: {}", announceUrl);
        if (isHttpTracker.test(announceUrl)) {
          maybeTrackerResponse = httpTrackerClient.requestPeers(announceUrl, torrent);
        } else if (isUDPTracker.test(announceUrl)) {
          maybeTrackerResponse = udpTrackerClient.requestPeers(announceUrl, torrent);
        }
        if (maybeTrackerResponse.isPresent()) {
          var trackerResponse = maybeTrackerResponse.get();
          trackerResponse.getPeersAddresses().ifPresent(this.peersStore::refreshPeers);

          // schedule the next peer collection
          collectionTaskFuture = torrent.getScheduledExecutorService()
              .schedule(peersCollectionTask, trackerResponse.getInterval(), TimeUnit.SECONDS);
          return;
        }
      }
      throw new RuntimeException("Failed to fetch peers from tracker!!");
    } catch (Exception e) {
      log.error("Error occurred while fetching peers!!", e);
      torrent.shutdown();
    }
  }

  @Override
  public void stop() {
    this.collectionTaskFuture.cancel(true);
  }

  private record PeersCollectionTask(TorrentPeersCollector torrentPeersCollector) implements
      Runnable {

    @Override
      public void run() {
        torrentPeersCollector.collectPeers();
      }
    }
}
