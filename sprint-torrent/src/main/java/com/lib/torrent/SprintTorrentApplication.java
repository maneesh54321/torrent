package com.lib.torrent;

import com.dampcake.bencode.Bencode;
import com.lib.torrent.content.ContentManager;
import com.lib.torrent.content.ContentManagerRandomAccessFileImpl;
import com.lib.torrent.downloader.nonblocking.TorrentDownloaderNonBlocking;
import com.lib.torrent.parser.MetaInfo;
import com.lib.torrent.peers.PeersCollector;
import com.lib.torrent.piece.AvailablePieceStore;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Selector;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.core.io.ClassPathResource;

public class SprintTorrentApplication {

  private final static Bencode bencode = new Bencode(true);

  public static void main(String[] args) throws IOException {

//    InputStream inputStream = new ClassPathResource("debian-9.3.0-ppc64el-netinst.torrent").getInputStream();
    InputStream inputStream = new ClassPathResource("Adam Lambert - High Drama (2023) Mp3 320kbps.torrent").getInputStream();
//    InputStream inputStream = new ClassPathResource("Farzi.S01.Complete.720p.AMZN.WEBRip.AAC.H.265-HODL.torrent").getInputStream();

    // read the torrent file
    MetaInfo metaInfo = MetaInfo.parseTorrentFile(inputStream);

    AtomicBoolean downloadCompleted = new AtomicBoolean(false);

    PeersCollector peersCollector = new PeersCollector(bencode, metaInfo, downloadCompleted);

    AvailablePieceStore availablePieceStore = new AvailablePieceStore(metaInfo);

    ContentManager contentManager = new ContentManagerRandomAccessFileImpl(metaInfo);

//    TorrentDownloader downloader = new TorrentDownloader(peersCollector, metaInfo,
//        availablePieceStore, contentManager, downloadCompleted);

    TorrentDownloaderNonBlocking torrentDownloaderNonBlocking = new TorrentDownloaderNonBlocking(
        Selector.open(), metaInfo, peersCollector
    );


//    peersCollector.registerListener(downloader);
    peersCollector.registerListener(torrentDownloaderNonBlocking);

    peersCollector.start();

//    connectToPeer(trackerResponse.getPeers().get(10), metaInfo.getInfo().getInfoHash(), PEER_ID);
  }

}
