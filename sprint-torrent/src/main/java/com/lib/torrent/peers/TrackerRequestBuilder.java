package com.lib.torrent.peers;

interface InfoHashTrackerRequestBuilder {

  PeerIdTrackerRequestBuilder withInfoHash(byte[] infoHash);
}

interface PeerIdTrackerRequestBuilder {

  PortTrackerRequestBuilder withPeerId(String peerId);
}

interface PortTrackerRequestBuilder {

  UploadedTrackerRequestBuilder withPort(int port);
}

interface UploadedTrackerRequestBuilder {

  DownloadedTrackerRequestBuilder withUploaded(long uploaded);
}

interface DownloadedTrackerRequestBuilder {

  LeftTrackerRequestBuilder withDownloaded(long downloaded);
}

interface LeftTrackerRequestBuilder {

  FinalTrackerRequestBuilder withLeft(long left);
}

interface FinalTrackerRequestBuilder {

  TrackerRequest build();
}

public class TrackerRequestBuilder implements InfoHashTrackerRequestBuilder,
    PeerIdTrackerRequestBuilder, PortTrackerRequestBuilder, UploadedTrackerRequestBuilder,
    DownloadedTrackerRequestBuilder, LeftTrackerRequestBuilder, FinalTrackerRequestBuilder {

  private byte[] infoHash;

  private String peerId;

  private int port;

  private long uploaded;

  private long downloaded;

  private long left;

  private Boolean compact;

  private Boolean noPeerId;

  public TrackerRequestBuilder() {
  }

  public static InfoHashTrackerRequestBuilder  aTrackerRequest() {
    return new TrackerRequestBuilder();
  }

  public TrackerRequest build() {
    TrackerRequest request = new TrackerRequest(infoHash, peerId, port, uploaded, downloaded, left);

    if (compact != null) {
      request.setCompact(compact ? 1 : 0);
    }

    if (noPeerId != null) {
      request.setNoPeerId(noPeerId ? 1 : 0);
    }

    return request;
  }

  public TrackerRequestBuilder compact(boolean compact) {
    this.compact = compact;
    return this;
  }

  public TrackerRequestBuilder noPeerId(boolean noPeerId) {
    this.noPeerId = noPeerId;
    return this;
  }

  @Override
  public PeerIdTrackerRequestBuilder withInfoHash(byte[] infoHash) {
    this.infoHash = infoHash;
    return this;
  }

  @Override
  public PortTrackerRequestBuilder withPeerId(String peerId) {
    this.peerId = peerId;
    return this;
  }

  @Override
  public UploadedTrackerRequestBuilder withPort(int port) {
    this.port = port;
    return this;
  }

  @Override
  public DownloadedTrackerRequestBuilder withUploaded(long uploaded) {
    this.uploaded = uploaded;
    return this;
  }

  @Override
  public LeftTrackerRequestBuilder withDownloaded(long downloaded) {
    this.downloaded = downloaded;
    return this;
  }

  @Override
  public FinalTrackerRequestBuilder withLeft(long left) {
    this.left = left;
    return this;
  }
}
