package com.lib.torrent.content;

import com.lib.torrent.common.Constants;
import com.lib.torrent.downloader.DownloadedBlock;
import com.lib.torrent.parser.DownloadFile;
import com.lib.torrent.parser.MetaInfo;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentManagerRandomAccessFileImpl implements ContentManager {

  private static final Logger log = LoggerFactory.getLogger(
      ContentManagerRandomAccessFileImpl.class);

  private final MetaInfo metaInfo;

  private RandomAccessFile randomAccessFile;

  public ContentManagerRandomAccessFileImpl(MetaInfo metaInfo) throws FileNotFoundException {
    this.metaInfo = metaInfo;
    List<DownloadFile> downloadFiles = metaInfo.getInfo().getDownloadFiles();
    if (downloadFiles.size() == 1) {
      randomAccessFile = new RandomAccessFile(
          Constants.DOWNLOAD_ROOT_LOCATION + downloadFiles.get(0).getName(), "rw");
    }
  }

  @Override
  public void writeToDisk(DownloadedBlock downloadedBlock) throws IOException {
    log.debug("Writing to disk: {}", downloadedBlock);

    long offset = downloadedBlock.pieceIndex() * metaInfo.getInfo().getPieceLength()
        + downloadedBlock.offset();

    randomAccessFile.seek(offset);

    randomAccessFile.write(downloadedBlock.data());
  }

  public void shutdown() {
    try {
      randomAccessFile.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
