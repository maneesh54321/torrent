package com.lib.torrent;

import com.dampcake.bencode.Bencode;
import com.lib.torrent.core.LongRunningProcess;
import com.lib.torrent.downloader.Message;
import com.lib.torrent.downloader.TorrentDownloader;
import com.lib.torrent.parser.MetaInfo;
import com.lib.torrent.peers.Listener;
import com.lib.torrent.peers.Peer;
import com.lib.torrent.peers.PeersCollector;
import com.lib.torrent.peers.PeersStore;
import com.lib.torrent.peers.Subject;
import com.lib.torrent.utils.BinaryDataUtils;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class SprintTorrentApplication {

    private final static Bencode bencode = new Bencode(true);

    public static void main(String[] args) throws IOException, InterruptedException {

        InputStream inputStream = new ClassPathResource("debian-9.3.0-ppc64el-netinst.torrent").getInputStream();
//        InputStream inputStream = new ClassPathResource("Farzi.S01.Complete.720p.AMZN.WEBRip.AAC.H.265-HODL.torrent").getInputStream();
        // read the torrent file
        MetaInfo metaInfo = MetaInfo.parseTorrentFile(inputStream);

        AtomicBoolean downloadCompleted = new AtomicBoolean(false);

        LongRunningProcess peersCollector = new PeersCollector(bencode, metaInfo, downloadCompleted);

        peersCollector.start();

        Subject peerCollectorSubject = (Subject) peersCollector;

        Listener downloader = new TorrentDownloader(1, (PeersStore) peersCollector);

        peerCollectorSubject.registerListener(downloader);



//        connectToPeer(trackerResponse.getPeers().get(10), metaInfo.getInfo().getInfoHash(), PEER_ID);
    }

    public static void connectToPeer(Peer peer, byte[] infoHash, String peerId) {
        SocketAddress socketAddress = new InetSocketAddress(peer.getIpAddress(), peer.getPort());

        try (SocketChannel client = SocketChannel.open();
             Selector selector = Selector.open()){
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_CONNECT);

            client.connect(socketAddress);

            boolean handshakeCompleted = false;

            boolean interested = false;

            boolean readyToDownload = false;

            ByteBuffer response = ByteBuffer.allocate(512);

            while(true) {
                selector.selectNow();
                var selectedKeys = selector.selectedKeys().iterator();

                while (selectedKeys.hasNext()) {
                    var selectedKey = selectedKeys.next();
                    if(selectedKey.isConnectable()) {
                        System.out.println("connected!!!");
                        var channel = (SocketChannel) selectedKey.channel();
                        var isConnected = channel.finishConnect();
                        Assert.isTrue(isConnected, "Client should correctly handle connect");
                        selectedKey.interestOps(SelectionKey.OP_WRITE);
                    } else if (selectedKey.isWritable()) {
                        if(!handshakeCompleted) {
                            System.out.println("Sending handshake ....");
                            int wroteBytesNum = client.write(Message.buildHandshake(infoHash, peerId));
                            System.out.println("wrote " + wroteBytesNum + " bytes");
                            selectedKey.interestOps(SelectionKey.OP_READ);
                        } else if(!interested){
                            System.out.println("Sending interested ....");
                            int wroteBytesNum = client.write(Message.buildInterested().flip());
                            System.out.println("wrote " + wroteBytesNum + " bytes");
                            interested = true;
                            selectedKey.interestOps(SelectionKey.OP_READ);
                        } else if (readyToDownload) {
                            System.out.println("Sending request to download piece: 0");
                            int wroteBytesNum = client.write(Message.buildDownloadRequest().flip());
                            System.out.println("wrote " + wroteBytesNum + " bytes");
                            selectedKey.interestOps(SelectionKey.OP_READ);
                        }
                    } else if (selectedKey.isReadable()) {
                        int bytesRead = client.read(response);
                        System.out.println("Bytes read: " + bytesRead);

                        response.flip();

                        while (response.hasRemaining()) {
                            byte[] wholeMessage = extractWholeMessage(response, handshakeCompleted);
                            if(!handshakeCompleted) {
                                int pstrlen = wholeMessage[0];
                                String pstr = new String(wholeMessage, 1, pstrlen, StandardCharsets.UTF_8);
                                if(pstr.equals("BitTorrent protocol")){
                                    System.out.println("this is handshake!!!");
                                    handshakeCompleted = true;
                                    System.out.println("Handshake completed!!!");
                                }
                            } else {
                                if(wholeMessage.length == 0) {
                                    System.out.println("Keep alive message received!!!");
                                } else {
                                    int messageId = wholeMessage[0];
                                    switch (messageId) {
                                        case 0 -> System.out.println("Choke message received!!!");
                                        case 1 -> {
                                            System.out.println("Unchoke message received!!!");
                                            readyToDownload = true;
                                        }
                                        case 2 -> System.out.println("Interested message received!!!");
                                        case 3 -> System.out.println("Uninterested message received!!!");
                                        case 5 -> {
                                            System.out.println("BitField message received!!!");
                                            System.out.println("BitField Message Length: " + wholeMessage.length);
                                            System.out.println(BinaryDataUtils.toBinaryString(wholeMessage, 1,
                                                    wholeMessage.length-1));
                                        }
                                        case 7 -> System.out.println("Piece received!!!");
                                        default ->
                                                System.out.println("Unknown message received: message Id: " + messageId);
                                    }
                                }
                            }

                            System.out.println("remaining bytes in response: " + response.remaining());
                        }

                        response.clear();

                        selectedKey.interestOps(SelectionKey.OP_WRITE);
                    }

                    selectedKeys.remove();
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] extractWholeMessage(ByteBuffer response, boolean handshakeCompleted) {
        byte[] wholeMessage = new byte[0];
        if(!handshakeCompleted){
            wholeMessage = new byte[68];
            response.get(wholeMessage);
        } else {
            int length = response.getInt();
            if(response.remaining() >= length) {
                wholeMessage = new byte[length];
                response.get(wholeMessage);
            }
        }
        return wholeMessage;
    }


}
