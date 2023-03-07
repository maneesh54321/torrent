package com.maneesh.peers.impl.http;

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

  HttpTrackerRequest build();

  FinalTrackerRequestBuilder compact(int compact);

  FinalTrackerRequestBuilder noPeerId(int noPeerId);
}

class TrackerRequestBuilder implements InfoHashTrackerRequestBuilder,
    PeerIdTrackerRequestBuilder, PortTrackerRequestBuilder, UploadedTrackerRequestBuilder,
    DownloadedTrackerRequestBuilder, LeftTrackerRequestBuilder, FinalTrackerRequestBuilder {

  private byte[] infoHash;

  private String peerId;

  private int port;

  private long uploaded;

  private long downloaded;

  private long left;

  private Integer compact;

  private Integer noPeerId;

  public TrackerRequestBuilder() {
  }

  public static InfoHashTrackerRequestBuilder aTrackerRequest() {
    return new TrackerRequestBuilder();
  }

  public HttpTrackerRequest build() {
    HttpTrackerRequest request = new HttpTrackerRequest(infoHash, peerId, port, uploaded, downloaded, left);

    if (compact != null) {
      request.setCompact(compact);
    }

    if (noPeerId != null) {
      request.setNoPeerId(noPeerId);
    }

    return request;
  }

  @Override
  public FinalTrackerRequestBuilder compact(int compact) {
    this.compact = compact;
    return null;
  }

  @Override
  public FinalTrackerRequestBuilder noPeerId(int noPeerId) {
    this.noPeerId = noPeerId;
    return null;
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
