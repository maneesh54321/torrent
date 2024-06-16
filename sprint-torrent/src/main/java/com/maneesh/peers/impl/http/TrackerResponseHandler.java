package com.maneesh.peers.impl.http;

import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import com.maneesh.peers.impl.TrackerResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TrackerResponseHandler {

  private static final Logger log = LoggerFactory.getLogger(TrackerResponseHandler.class);

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

  public Optional<TrackerResponse> extractTrackerResponse(byte[] bytes) {
    try {
      Map<String, Object> map = bencode.decode(bytes, Type.DICTIONARY);

      log.info("Tracker response: {}", map);

      var byteBuffer = (ByteBuffer) map.get("peers");
      var trackerResponse = new TrackerResponse();
      trackerResponse.setInterval((Long) map.get("interval"));
      trackerResponse.setLechers((Long) map.get("incomplete"));
      trackerResponse.setSeeders((Long) map.get("complete"));

      while (byteBuffer.hasRemaining()) {
        var ip = new StringBuilder();
        for (var i = 0; i < 4; i++) {
          ip.append(byteBuffer.get() & 0xff);
          ip.append(".");
        }
        ip.deleteCharAt(ip.length() - 1);
        var port = 0;
        for (var i = 0; i < 2; i++) {
          port = (port << 8) + (byteBuffer.get() & 0xFF);
        }
        trackerResponse.addPeer(ip.toString(), port);
      }

      return Optional.of(trackerResponse);
    } catch (Exception e) {
      log.error("Error occurred while parsing tracker response!!", e);
      return Optional.empty();
    }
  }

  public void extractTrackerResponse(String input) {
    extractTrackerResponse(input.getBytes(StandardCharsets.UTF_8));
  }
}
