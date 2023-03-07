package com.maneesh.peers.impl;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.collections4.CollectionUtils;

public class TrackerResponse {
  private Long interval;

  private Long lechers;

  private Long seeders;

  private List<SocketAddress> peerAddresses;

  public long getInterval() {
    return interval;
  }

  public void setInterval(Long interval) {
    this.interval = interval;
  }

  public void setLechers(Long lechers) {
    this.lechers = lechers;
  }

  public void setSeeders(Long seeders) {
    this.seeders = seeders;
  }

  public Optional<List<SocketAddress>> getPeersAddresses() {
    return Optional.ofNullable(peerAddresses);
  }

  public void addPeer(String host, int port) {
    if (CollectionUtils.isEmpty(peerAddresses)){
      peerAddresses = new ArrayList<>();
    }
    peerAddresses.add(new InetSocketAddress(host, port));
  }

  @Override
  public String toString() {
    return "TrackerResponse{" +
        "interval=" + interval +
        ", peers=" + peerAddresses +
        '}';
  }
}

