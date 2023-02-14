package com.lib.torrent.peers;

import com.dampcake.bencode.Bencode;
import com.lib.torrent.parser.MetaInfo;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class PeersCollector implements Subject, PeersStore {

  private static final Duration DEFAULT_PEERS_COLLECTION_TIME = Duration.of(30, ChronoUnit.SECONDS);
  private final static String PEER_ID = "-SP1000-uartcg486259";
  private final static int PORT = 6881;
  private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

  private final Set<Peer> peers;

  private final MetaInfo metaInfo;

  private final AtomicBoolean downloadCompleted;

  private final TrackerResponseHandler trackerResponseHandler;

  private final List<Listener> listeners = new ArrayList<>();

  public PeersCollector(Bencode bencode, MetaInfo metaInfo, AtomicBoolean downloadCompleted) {
    peers = new HashSet<>();
    this.metaInfo = metaInfo;
    this.downloadCompleted = downloadCompleted;
    this.trackerResponseHandler = TrackerResponseHandler.getInstance(bencode);
  }

  public void collectPeers(MetaInfo metaInfo) {
    try {
      // request tracker for list of peers.
      TrackerResponse trackerResponse = requestTrackerForPeers(metaInfo);
      List<Peer> peers = trackerResponse.getPeers().orElse(Collections.emptyList());
      this.addPeers(peers);
      if (downloadCompleted.get()) {
        scheduledExecutorService.shutdownNow();
      } else {
        scheduledExecutorService.schedule(new PeersCollectorTask(this),
            trackerResponse.getInterval().orElse(DEFAULT_PEERS_COLLECTION_TIME.getSeconds()),
            TimeUnit.SECONDS);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void stopCollection() {
    scheduledExecutorService.shutdownNow();
  }

  private TrackerResponse requestTrackerForPeers(MetaInfo metaInfo)
      throws IOException {

    // form tracker request and send it to Tracker

    // Will need to update it according to the download progress
    int uploaded = 0;
    int downloaded = 0;

    System.out.println("Building request...");

    TrackerRequest trackerRequest = TrackerRequestBuilder.aTrackerRequest()
        .withInfoHash(metaInfo.getInfo().getInfoHash()).withPeerId(PEER_ID).withPort(PORT)
        .withUploaded(uploaded).withDownloaded(downloaded).withLeft(0).build();

    String trackerRequestUrl = trackerRequest.getUrlEncodedUrl(metaInfo.getAnnounce());

    System.out.println("Tracker Request URL: " + trackerRequestUrl);

    URL tracker = new URL(trackerRequestUrl);

    System.out.println("Sending message to tracker...");

    var con = (HttpURLConnection) tracker.openConnection();
    con.setRequestMethod("GET");
    con.setRequestProperty("Connection", "close");
    con.setRequestProperty("Accept-Encoding", "gzip");

    var dataIs = con.getInputStream();

    byte[] bytes = dataIs.readAllBytes();

    TrackerResponse trackerResponse = trackerResponseHandler.extractTrackerResponse(bytes);

    System.out.println(trackerResponse);

    return trackerResponse;
  }

  @Override
  public void registerListener() {

  }

  @Override
  public void removeListener() {

  }

  @Override
  public void notifyListeners() {
    listeners.forEach(Listener::update);
  }

  public Set<Peer> getPeers() {
    return peers;
  }

  @Override
  public void addPeers(Collection<Peer> peers) {
    this.peers.addAll(peers);
    notifyListeners();
  }

  static class PeersCollectorTask implements Runnable {

    private final PeersCollector peersCollector;

    public PeersCollectorTask(PeersCollector peersCollector) {
      this.peersCollector = peersCollector;
    }

    @Override
    public void run() {
      peersCollector.collectPeers(peersCollector.metaInfo);
    }
  }
}
