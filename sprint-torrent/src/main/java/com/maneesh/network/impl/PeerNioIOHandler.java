package com.maneesh.network.impl;

import com.maneesh.core.LongRunningProcess;
import com.maneesh.core.Peer;
import com.maneesh.core.Torrent;
import com.maneesh.network.PeerIOHandler;
import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PeerNioIOHandler implements PeerIOHandler, LongRunningProcess {

  private final Torrent torrent;

  private final Selector connectionSelector;

  private final ScheduledFuture<?> ioPollingTask;

  public PeerNioIOHandler(Torrent torrent) throws IOException {
    connectionSelector = Selector.open();
    this.torrent = torrent;
    ioPollingTask = torrent.getScheduledExecutorService()
        .scheduleAtFixedRate(this::pollConnectionsIO, 50, 50, TimeUnit.MILLISECONDS);
  }

  private void pollConnectionsIO() {
    try {
      connectionSelector.select(selectionKey -> {

      });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void registerConnection(SocketChannel socketChannel, Peer peer) {

  }

  @Override
  public void onDataAvailable() {

  }

  @Override
  public void stop() {
    ioPollingTask.cancel(true);
  }
}
