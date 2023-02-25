package com.lib.torrent.peers;

import java.util.Optional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

class TrackerResponse {
    private Long interval;

    private int leechers;

    private int seeders;

    private List<Peer> peers;

    public Optional<Long> getInterval() {
        return Optional.ofNullable(interval);
    }

    public void setInterval(Long interval) {
        this.interval = interval;
    }

    public void setLeechers(int leechers) {
        this.leechers = leechers;
    }

    public void setSeeders(int seeders) {
        this.seeders = seeders;
    }

    public Optional<List<Peer>> getPeers() {
        return Optional.ofNullable(peers);
    }

    public void addPeer(Peer peer) {
        if (CollectionUtils.isEmpty(peers)){
            peers = new ArrayList<>();
        }
        peers.add(peer);
    }

    @Override
    public String toString() {
        return "TrackerResponse{" +
                "interval=" + interval +
                ", peers=" + peers +
                '}';
    }
}
