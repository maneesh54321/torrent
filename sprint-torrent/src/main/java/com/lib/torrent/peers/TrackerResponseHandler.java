package com.lib.torrent.peers;

import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

class TrackerResponseHandler {

  private static TrackerResponseHandler trackerResponseHandler;
  private final Bencode bencode;

  private TrackerResponseHandler(Bencode bencode) {
    this.bencode = bencode;
  }

  public static TrackerResponseHandler getInstance(Bencode bencode) {
    if(trackerResponseHandler == null) {
        synchronized (TrackerResponseHandler.class) {
            if(trackerResponseHandler == null){
                trackerResponseHandler = new TrackerResponseHandler(bencode);
            }
        }
    }
    return trackerResponseHandler;
  }

  public TrackerResponse extractTrackerResponse(byte[] bytes) {
    Map<String, Object> map = bencode.decode(bytes, Type.DICTIONARY);

    System.out.println("Tracker response: " + map);

    ByteBuffer byteBuffer = (ByteBuffer) map.get("peers");
    TrackerResponse trackerResponse = new TrackerResponse();
    trackerResponse.setInterval((Long) map.get("interval"));
    while (byteBuffer.hasRemaining()) {
      StringBuilder ip = new StringBuilder();
      for (int i = 0; i < 4; i++) {
        ip.append(byteBuffer.get() & 0xff);
        ip.append(".");
      }
      ip.deleteCharAt(ip.length() - 1);
      int port = 0;
      for (int i = 0; i < 2; i++) {
        port = (port << 8) + (byteBuffer.get() & 0xFF);
      }
      Peer peer = new Peer(ip.toString(), port);
      trackerResponse.addPeer(peer);
    }

    return trackerResponse;
  }

  public void extractTrackerResponse(String input) {
    extractTrackerResponse(input.getBytes(StandardCharsets.UTF_8));
  }
}
