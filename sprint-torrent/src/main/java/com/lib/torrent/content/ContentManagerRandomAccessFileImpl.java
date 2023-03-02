package com.lib.torrent.content;

import com.lib.torrent.common.Constants;
import com.lib.torrent.downloader.DownloadedBlock;
import com.lib.torrent.parser.DownloadFile;
import com.lib.torrent.parser.MetaInfo;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentManagerRandomAccessFileImpl implements ContentManager {

  private static final Logger log = LoggerFactory.getLogger(
      ContentManagerRandomAccessFileImpl.class);

  private final MetaInfo metaInfo;

  private final int[] startIndices;

  private final RandomAccessFile[] randomAccessFileList;

  private final ContentMode contentMode;

  public ContentManagerRandomAccessFileImpl(MetaInfo metaInfo) throws IOException {
    this.metaInfo = metaInfo;
    Content content = metaInfo.getInfo().getContent();

    DownloadFile[] downloadFiles = content.downloadFiles();
    startIndices = new int[downloadFiles.length];
    this.randomAccessFileList = new RandomAccessFile[downloadFiles.length];

    if (downloadFiles.length > 1) {
      // multi file mode
      contentMode = ContentMode.MULTI;
      for (int i = 0; i < downloadFiles.length; i++) {
        DownloadFile downloadFile = downloadFiles[i];
        startIndices[i] = downloadFile.getPieceStartIndex();
        Path rootDirectory = Path.of(
            Constants.DOWNLOAD_ROOT_LOCATION + content.rootDirectoryName());
        if (!Files.exists(rootDirectory)) {
          Files.createDirectory(rootDirectory);
        }
        int j = 0;
        StringBuilder path = new StringBuilder();
        while (j < downloadFile.getPath().size() - 1) {
          path.append("/").append(downloadFile.getPath().get(j));
          Path filePath = Path.of(
              Constants.DOWNLOAD_ROOT_LOCATION + content.rootDirectoryName() + path);
          if(!Files.exists(filePath)){
            Files.createDirectory(filePath);
          }
          j++;
        }
        randomAccessFileList[i] = new RandomAccessFile(
            Constants.DOWNLOAD_ROOT_LOCATION + content.rootDirectoryName() + path + "/"
                + downloadFile.getName(),
            "rw");
      }
    } else {
      contentMode = ContentMode.SINGLE;
      // single file mode
      randomAccessFileList[0] = new RandomAccessFile(
          Constants.DOWNLOAD_ROOT_LOCATION + downloadFiles[0].getName(), "rw");
    }
  }

  @Override
  public void writeToDisk(DownloadedBlock downloadedBlock) throws IOException {
    log.debug("Writing to disk: {}", downloadedBlock);

    // search the file to which this block belongs
    int fileIndex = contentMode == ContentMode.SINGLE ? 0
        : findFileIndexByPieceIndex(downloadedBlock.pieceIndex());

    RandomAccessFile file = randomAccessFileList[fileIndex];

    long offset = (downloadedBlock.pieceIndex() - startIndices[fileIndex]) * metaInfo.getInfo().getPieceLength()
        + downloadedBlock.offset();

    file.seek(offset);

    file.write(downloadedBlock.data());
  }

  private int findFileIndexByPieceIndex(int pieceIndex) {
    int l = 0, r = startIndices.length - 1;
    int mid;
    while (true) {
      if (r - l == 1) {
        return l;
      }
      mid = l + (r - l) / 2;
      if (startIndices[mid] == pieceIndex) {
        return mid;
      } else if (startIndices[mid] < pieceIndex) {
        l = mid;
      } else {
        r = mid;
      }
    }
  }

  public void shutdown() {
    try {
      for (RandomAccessFile raf : randomAccessFileList) {
        raf.close();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
