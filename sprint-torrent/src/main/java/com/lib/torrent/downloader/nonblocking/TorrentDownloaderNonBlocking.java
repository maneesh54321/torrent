package com.lib.torrent.downloader.nonblocking;

import com.lib.torrent.downloader.Message;
import com.lib.torrent.downloader.MessageType;
import com.lib.torrent.downloader.exception.ConnectionChokedException;
import com.lib.torrent.downloader.exception.ConnectionFailedException;
import com.lib.torrent.parser.MetaInfo;
import com.lib.torrent.peers.Listener;
import com.lib.torrent.peers.Peer;
import com.lib.torrent.peers.PeersCollector;
import com.lib.torrent.piece.AvailablePieceStore;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TorrentDownloaderNonBlocking implements Listener {

  private static final Logger log = LoggerFactory.getLogger(TorrentDownloaderNonBlocking.class);

  private final MetaInfo metaInfo;

  private final AvailablePieceStore availablePieceStore;

  private final PeersCollector peersCollector;

  private final Selector selector;

  private final Map<SelectableChannel, Connection> map;

  private final ByteBuffer lengthBuffer = ByteBuffer.allocate(4);

  private final DownloadingPiece downloadingPiece;

  public TorrentDownloaderNonBlocking(Selector selector, MetaInfo metaInfo, PeersCollector peersCollector) {
    this.selector = selector;
    this.metaInfo = metaInfo;
    this.availablePieceStore = new AvailablePieceStore(metaInfo);
    map = new HashMap<>();
    this.peersCollector = peersCollector;
    downloadingPiece = new DownloadingPiece(availablePieceStore);
  }

  public static void processConnect(SelectionKey key) throws ConnectionFailedException {
    SocketChannel sc = (SocketChannel) key.channel();
    try {
      while (sc.isConnectionPending()) {
        sc.finishConnect();
      }
    } catch (IOException e) {
      key.cancel();
      throw new ConnectionFailedException("Failed to connect!!!", e);
    }
  }

  public void start(Collection<Peer> peers) throws IOException {
//    peers.stream().limit(10).forEach(peer -> {
//      SocketChannel channel = null;
//      try {
//        channel = SocketChannel.open();
//        channel.configureBlocking(true);
//        channel.connect(new InetSocketAddress(peer.getIpAddress(), peer.getPort()));
//
//        // send handshake message
//        log.debug("Wrote {} bytes", channel.write(Message.buildHandshake(metaInfo.getInfo()
//            .getInfoHash(), Constants.PEER_ID).flip()));
//
//        ByteBuffer handshake = ByteBuffer.allocate(68);
//        log.debug("No of bytes read : {}", channel.read(handshake));
//        verifyHandshake(handshake.flip());
//
//        // send interested message
//        channel.write(Message.buildInterested());
//
//        // TODO set am_interested
//
//        channel.configureBlocking(false);
//
//        channel.register(selector, SelectionKey.OP_WRITE);
//
//        map.put(channel, new Connection(peer, metaInfo));
//      } catch (Exception e) {
//        log.warn("Exception occurred while connecting to Peer: {}", peer, e);
//        try {
//          if (channel.isOpen()) {
//            channel.close();
//          }
//        } catch (IOException ex) {
//          throw new RuntimeException(ex);
//        }
//      }
//    });

    peers.stream().limit(30).forEach(peer -> {
      try {
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_CONNECT);
        channel.connect(new InetSocketAddress(peer.getIpAddress(), peer.getPort()));
        map.put(channel, new Connection(peer, metaInfo, downloadingPiece, availablePieceStore));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

    while (true) {
      selector.select(key -> {
        try {
          SocketChannel sc = (SocketChannel) key.channel();
          Connection connection = map.get(sc);
          if (key.isValid()) {
            if (key.isConnectable()) {
              processConnect(key);
              key.interestOps(SelectionKey.OP_WRITE);
            }
            if (key.isReadable()) {
              connection.handleResponse(sc);
              key.interestOps(SelectionKey.OP_WRITE).interestOpsOr(SelectionKey.OP_READ);
            }
            if (key.isWritable()) {
              ByteBuffer bb = connection.getRequestMessage();
              if(bb != null) {
                sc.write(bb);
              }
              key.interestOps(SelectionKey.OP_READ);
            }
          }
        } catch (ConnectionFailedException e) {
          log.error(e.getMessage(), e);
        } catch (IOException e) {
          log.error("Error occurred while interacting with a Peer.", e);
          key.cancel();
        } catch (Exception e) {
          log.error("Connection failed!!!");
          key.cancel();
        }
      }, 3000);
    }
//    handleAllConnections();
  }

//  public void download(AvailablePiece availablePiece) throws IOException {
//    boolean downloading = true;
//    PieceRequest pieceRequest = new PieceRequest(availablePiece.getPieceIndex(), metaInfo);
//    Iterator<BlockRequest> blockRequestIterator = pieceRequest.getBlockRequests().iterator();
//    List<DownloadedBlock> downloadedBlocks = new ArrayList<>();
//    while (blockRequestIterator.hasNext()) {
//      selector.selectNow();
//      var selectedKeys = selector.selectedKeys().iterator();
//      while (selectedKeys.hasNext()) {
//        var selectedKey = selectedKeys.next();
//        SocketChannel channel = (SocketChannel) selectedKey.channel();
//        Connection peerConnection = map.get(channel);
//        if (peerConnection.hasPiece(availablePiece.getPieceIndex())) {
//          if (selectedKey.isWritable()) {
//            BlockRequest blockRequest = blockRequestIterator.next();
//            channel.write(
//                Message.buildDownloadRequest(blockRequest.getPieceIndex(), blockRequest.getOffset(),
//                    blockRequest.getLength()).flip());
//            selectedKey.interestOps(SelectionKey.OP_READ);
//          } else if (selectedKey.isReadable()) {
//            channel.read(lengthBuffer);
//            int length = lengthBuffer.getInt();
//            lengthBuffer.flip();
//            ByteBuffer dataBuffer = ByteBuffer.allocate(length);
//            channel.read(dataBuffer);
//            dataBuffer.flip();
//            downloadedBlocks.add(new DownloadedBlock());
//          }
//        }
//
//      }
//    }
//    while (downloading) {
//      selector.selectNow();
//      var selectedKeys = selector.selectedKeys().iterator();
//      while (selectedKeys.hasNext()) {
//        var selectedKey = selectedKeys.next();
//
//        SocketChannel channel = (SocketChannel) selectedKey.channel();
//        Connection peerConnection = map.get(channel);
//
//        if (availablePiece.getPeers().contains(peerConnection.getPeer())) {
//          if (selectedKey.isValid()) {
//            if (selectedKey.isWritable()) {
//              channel.write(Message.buildDownloadRequest(availablePiece.getPieceIndex(), ));
//            } else if (selectedKey.isWritable()) {
////              if (peerConnection.isReadyToDownload()) {
////                System.out.println("Sending request to download piece: 0");
////                int wroteBytesNum = channel.write(Message.buildDownloadRequest().flip());
////                System.out.println("wrote " + wroteBytesNum + " bytes");
////                selectedKey.interestOps(SelectionKey.OP_READ);
////              }
//            } else if (selectedKey.isConnectable()) {
//
//            } else if (selectedKey.isAcceptable()) {
//
//            }
//          }
//        }
//      }
//    }
//  }

  private void handleAllConnections() {
    try {
      while (true) {
        selector.selectNow();
        var selectedKeys = selector.selectedKeys().iterator();

        while (selectedKeys.hasNext()) {
          var selectedKey = selectedKeys.next();

          SocketChannel channel = (SocketChannel) selectedKey.channel();
          Connection peerConnection = map.get(channel);

          if (selectedKey.isValid()) {
            if (selectedKey.isReadable()) {
              ByteBuffer message = readWholeMessage(channel);
              handleMessage(message, peerConnection, channel);
            } else if (selectedKey.isWritable()) {
//              if (peerConnection.isReadyToDownload()) {
//                System.out.println("Sending request to download piece: 0");
//                int wroteBytesNum = channel.write(Message.buildDownloadRequest().flip());
//                System.out.println("wrote " + wroteBytesNum + " bytes");
//                selectedKey.interestOps(SelectionKey.OP_READ);
//              }
            } else if (selectedKey.isConnectable()) {

            } else if (selectedKey.isAcceptable()) {

            }
          }
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void handleMessage(ByteBuffer message, Connection peerConnection, SocketChannel channel)
      throws Exception {
    MessageType messageType = MessageType.valueOf(message.get());
    switch (messageType) {
      case PIECE -> {
        log.debug("piece message received!!!");
        // TODO handle downloaded block
      }
      case HAVE -> {
        log.debug("Have message received!!!");
        // Add this peer as having the piece index received in message
        int pieceIndex = message.getInt();
        availablePieceStore.addAvailablePiece(pieceIndex, peerConnection.getPeer());
      }
      case PIECE_REQUEST -> log.debug("piece request message received!!!");
      case CHOKE -> {
        log.debug("choke message received!!!");
//        peerConnection.set_peer_choking(true);
        throw new ConnectionChokedException("Connection has been choked!!!");
      }
      case UNCHOKE -> {
        log.debug("unChoke message received!!!");
//        peerConnection.set_peer_choking(false);
      }
      case BITFIELD -> {
        log.debug("Bitfield message received!!!");

        BitSet bitField = BitSet.valueOf(message);

        for (int i = 0; i < bitField.length(); i++) {
          if (bitField.get(i)) {
            availablePieceStore.addAvailablePiece(i, peerConnection.getPeer());
          }
        }
      }
      case INTERESTED -> {
        log.info("interested message received!!!");
        peerConnection.set_peer_interested(true);
        channel.write(Message.buildUnChoke());
      }
      case NOT_INTERESTED -> {
        log.info("Not interested message received!!!");
        peerConnection.set_peer_interested(false);
      }
      case CANCEL -> log.info("cancel message received!!!");
    }
  }


  private ByteBuffer readWholeMessage(SocketChannel channel) throws IOException {
    channel.read(lengthBuffer);
    lengthBuffer.flip();
    int length = lengthBuffer.getInt();
    lengthBuffer.clear();
    ByteBuffer message = ByteBuffer.allocate(length);
    channel.read(message);
    return message.flip();
  }

  private void verifyHandshake(ByteBuffer handshake) throws Exception {
    try {
      // check if it is handshake
      if (handshake.get() == 19) {
        byte[] protocolBuffer = new byte[19];
        handshake.get(protocolBuffer);
        String maybeProtocolString = new String(protocolBuffer, 0, 19, StandardCharsets.UTF_8);
        if (maybeProtocolString.equals("BitTorrent protocol")) {
          // read empty 8 bytes
          handshake.getLong();
          // Check if the info hash matches and drop the peer if it doesn't
          byte[] peerInfoHashBuffer = new byte[20];
          handshake.get(peerInfoHashBuffer);
          if (MessageDigest.isEqual(metaInfo.getInfo().getInfoHash(), peerInfoHashBuffer)) {
            log.info("Completed handshake!!!");
            return;
          } else {
            log.error("Peer has invalid info hash!!!");
          }
        }
      }
      throw new Exception("Not a handshake message!!!");
    } catch (Exception e) {
      log.error("Handshake failed with Peer!!!", e);
      throw new Exception("Handshake failed with Peer!!!", e);
    }
  }

  @Override
  public void update() {
    log.info("New peers received!!");

    Set<Peer> peers = peersCollector.getPeers();

    try {
      start(peers);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  @Override
  public int compareTo(Listener o) {
    return 0;
  }
}
