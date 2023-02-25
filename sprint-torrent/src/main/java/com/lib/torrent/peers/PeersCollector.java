package com.lib.torrent.peers;

import com.dampcake.bencode.Bencode;
import com.lib.torrent.common.Constants;
import com.lib.torrent.core.LongRunningProcess;
import com.lib.torrent.downloader.Message;
import com.lib.torrent.parser.MetaInfo;
import com.lib.torrent.peers.exception.InvalidUDPResponse;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PeersCollector implements LongRunningProcess, Subject, PeersStore {

  private static final Logger log = LoggerFactory.getLogger(PeersCollector.class);

  private static final Duration DEFAULT_PEERS_COLLECTION_TIME =
      Duration.of(30, ChronoUnit.SECONDS);

  private static final Random random = new Random();

  private final ScheduledExecutorService scheduledExecutorService =
      Executors.newSingleThreadScheduledExecutor();

  private final Set<Peer> peers;

  private final MetaInfo metaInfo;

  private final AtomicBoolean downloadCompleted;

  private final TrackerResponseHandler trackerResponseHandler;

  private final List<Listener> listeners = new ArrayList<>();
  // Will need to update it according to the download progress
  private long uploaded = 0;

  private long downloaded = 0;

  private long left = 0;

  public PeersCollector(Bencode bencode, MetaInfo metaInfo, AtomicBoolean downloadCompleted) {
    peers = new HashSet<>();
    this.metaInfo = metaInfo;
    this.downloadCompleted = downloadCompleted;
    this.trackerResponseHandler = TrackerResponseHandler.getInstance(bencode);
  }

  private static String readIpAddress(ByteBuffer buffer) {
    return String.format("%d.%d.%d.%d", buffer.get() & 0xFF, buffer.get() & 0xFF,
        buffer.get() & 0xFF, buffer.get() & 0xFF);
  }

  private static int readPort(ByteBuffer buffer) {
    return ((buffer.get() & 0xFF) << 8) | (buffer.get() & 0xFF);
  }

  @Override
  public void start() {
    startCollection();
  }

  @Override
  public void stop() {
    scheduledExecutorService.shutdownNow();
  }

  private void startCollection() {
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
    } catch (Exception e) {
      stop();
      throw new RuntimeException(e);
    }
  }

  private TrackerResponse requestTrackerForPeers(MetaInfo metaInfo)
      throws Exception {

    // form tracker request and send it to Tracker
    Optional<TrackerResponse> trackerResponse = Optional.empty();
    for (String announceUrl : metaInfo.getAnnounceList()) {
      if (announceUrl.startsWith("http")) {
        trackerResponse = requestHTTPTracker(announceUrl);
      } else if (announceUrl.startsWith("udp")) {
        trackerResponse = requestUDPTracker(announceUrl);
      }
      if (trackerResponse.isPresent()) {
        break;
      }
    }
    return trackerResponse.orElseThrow(
        () -> new Exception("Could not fetch peers from any trackers"));
  }

  private Optional<TrackerResponse> requestHTTPTracker(String httpAnnounceUrl) {
    try {
      TrackerRequest trackerRequest = TrackerRequestBuilder.aTrackerRequest()
          .withInfoHash(metaInfo.getInfo().getInfoHash()).withPeerId(Constants.PEER_ID)
          .withPort(Constants.PORT)
          .withUploaded(uploaded).withDownloaded(downloaded)
          .withLeft(metaInfo.getInfo().getTotalSizeInBytes()).build();

      String trackerRequestUrl = trackerRequest.getUrlEncodedUrl(httpAnnounceUrl);

      log.info("Tracker Request URL: " + trackerRequestUrl);

      URL tracker = new URL(trackerRequestUrl);

      log.info("Sending message to tracker...");

      var con = (HttpURLConnection) tracker.openConnection();
      con.setRequestMethod("GET");
      con.setRequestProperty("Connection", "close");
      con.setRequestProperty("Accept-Encoding", "gzip");

      var dataIs = con.getInputStream();

      byte[] bytes = dataIs.readAllBytes();

      return trackerResponseHandler.extractTrackerResponse(bytes);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Optional<TrackerResponse> requestUDPTracker(String udpAnnounceUrl) {
    try (DatagramSocket udpSocket = new DatagramSocket()) {
      // create UDP socket
      URI uri = URI.create(udpAnnounceUrl);

      InetSocketAddress socketAddress = new InetSocketAddress(uri.getHost(), uri.getPort());

      Long connectionId = sendConnectMessage(udpSocket, socketAddress).orElseThrow(
          () -> new Exception("Failed to retrieve peers from tracker!!"));
      log.debug("UDP Connect message success!!");
      return sendAnnounceMessage(udpSocket, connectionId, socketAddress);

    } catch (Exception e) {
      log.warn("Failed.", e);
      return Optional.empty();
    }
  }

  private Optional<Long> sendConnectMessage(DatagramSocket udpSocket,
      InetSocketAddress socketAddress) {

    int n = 0;
    while (n < 8) {
      try {
        int transactionId = random.nextInt(1000);
        // create UDP packet with connect message to send
        DatagramPacket connectPacket = new DatagramPacket(
            Message.buildConnect(transactionId).array(), 16,
            socketAddress);

        udpSocket.setSoTimeout(15000 * 2 ^ n);

        // send connect message
        udpSocket.send(connectPacket);

        // parse connect message response
        byte[] buffer = new byte[16];
        udpSocket.receive(new DatagramPacket(buffer, 16));

        ByteBuffer connectResponse = ByteBuffer.wrap(buffer);

        // verify the response
        int action = connectResponse.getInt();
        if (action != 0) {
          log.warn("Received action: {}, sent action: {}", action, 0);
          throw new InvalidUDPResponse(
              "Connect response has invalid action!!");
        }
        int receivedTxnId = connectResponse.getInt();
        if (transactionId != receivedTxnId) {
          log.warn("Received transactionId: {}, sent transactionId: {}", receivedTxnId,
              transactionId);
          throw new InvalidUDPResponse(
              "Connect response has invalid transactionId!!!");
        }
        return Optional.of(connectResponse.getLong());
      } catch (SocketTimeoutException e) {
        log.warn("Connect timed out. retrying..", e);
        n++;
      } catch (InvalidUDPResponse e) {
        log.warn("Invalid connect response", e);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return Optional.empty();
  }

  private Optional<TrackerResponse> sendAnnounceMessage(DatagramSocket udpSocket,
      long connectionId, InetSocketAddress socketAddress) {
    int n = 0;
    while (n < 3) {
      try {
        udpSocket.setSoTimeout(15000 * (int) Math.pow(2, n));
        // send announce message
        int transactionId = random.nextInt(1000);
        DatagramPacket announcePacket = new DatagramPacket(
            Message.buildAnnounce(
                connectionId, metaInfo.getInfo().getInfoHash(),
                Constants.PORT, transactionId, uploaded, downloaded, left).array(),
            98, socketAddress);

        udpSocket.send(announcePacket);

        // receive the response
        byte[] annResBuffer = new byte[4096];
        DatagramPacket responsePacket = new DatagramPacket(annResBuffer, annResBuffer.length);
        udpSocket.receive(responsePacket);
        ByteBuffer announceResponse = ByteBuffer.wrap(annResBuffer);

        // verify the response
        if (responsePacket.getLength() < 20) {
          throw new InvalidUDPResponse("Expected response packet to have least size 20");
        }

        int action = announceResponse.getInt();
        if (action != 1) {
          throw new InvalidUDPResponse(
              String.format("Expected action 1 but received action %s", action));
        }

        int receivedTxnId = announceResponse.getInt();
        if (receivedTxnId != transactionId) {
          throw new InvalidUDPResponse(
              String.format("Expected transactionId %s but received %s", transactionId,
                  receivedTxnId));
        }
        log.debug("UDP announce message success");
        TrackerResponse trackerResponse = new TrackerResponse();
        trackerResponse.setInterval((long) announceResponse.getInt());
        trackerResponse.setLeechers(announceResponse.getInt());
        trackerResponse.setSeeders(announceResponse.getInt());

        int peerListOffset = 20;
        while (peerListOffset < responsePacket.getLength()) {
          String host = readIpAddress(announceResponse);
          int port = readPort(announceResponse);
          Peer peer = new Peer(host, port);
          trackerResponse.addPeer(peer);
          peerListOffset += 6;
        }
        return Optional.of(trackerResponse);
      } catch (InvalidUDPResponse e) {
        log.warn("Invalid response received", e);
        n++;
      } catch (SocketException e) {
        log.warn("Connect timed out.", e);
        n++;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return Optional.empty();
  }

  @Override
  public void registerListener(Listener listener) {
    this.listeners.add(listener);
    log.info("Number of listeners: " + this.listeners.size());
  }

  @Override
  public void removeListener(Listener listener) {
    this.listeners.remove(listener);
  }

  @Override
  public void notifyListeners() {
    log.info("Notifying all listeners: " + listeners.size());
    listeners.forEach(Listener::update);
  }

  public Set<Peer> getPeers() {
    return peers;
  }

  @Override
  public void addPeers(Collection<Peer> peers) {
    var newPeersReceived = this.peers.addAll(peers);
    if (newPeersReceived) {
      notifyListeners();
    }
  }

  static class PeersCollectorTask implements Runnable {

    private final PeersCollector peersCollector;

    public PeersCollectorTask(PeersCollector peersCollector) {
      this.peersCollector = peersCollector;
    }

    @Override
    public void run() {
      peersCollector.startCollection();
    }
  }
}
