package com.maneesh.content.impl;

import com.maneesh.content.ContentManager;
import com.maneesh.content.DownloadedBlock;
import com.maneesh.common.Constants;
import com.maneesh.core.Torrent;
import com.maneesh.meta.Content;
import com.maneesh.meta.DownloadFile;
import com.maneesh.meta.Info;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentManagerRandomAccessFileImpl implements ContentManager {
  private static final Logger log = LoggerFactory.getLogger(
      ContentManagerRandomAccessFileImpl.class);

  private final Info info;

  private final int[] startIndices;

  private final RandomAccessFile[] randomAccessFileList;

  private final ContentMode contentMode;

  private final ExecutorService executorService;

  private final Torrent torrent;

  private Lock lock;

  public ContentManagerRandomAccessFileImpl(Torrent torrent, ExecutorService executorService, Info info) throws IOException {
    this.torrent = torrent;
    this.info = info;
    this.executorService = executorService;
    this.lock = new ReentrantLock();
    Content content = info.getContent();

    DownloadFile[] downloadFiles = content.getDownloadFiles();
    startIndices = new int[downloadFiles.length];
    this.randomAccessFileList = new RandomAccessFile[downloadFiles.length];

    if (content.getRootDirectoryName() != null) {
      if(downloadFiles.length == 1){
        // single file inside some directory
        contentMode = ContentMode.SINGLE;
      } else {
        // multi file mode
        contentMode = ContentMode.MULTI;
      }
      for (int i = 0; i < downloadFiles.length; i++) {
        DownloadFile downloadFile = downloadFiles[i];
        startIndices[i] = downloadFile.getPieceStartIndex();
        Path rootDirectory = Path.of(
            Constants.DOWNLOAD_ROOT_LOCATION + content.getRootDirectoryName());
        if (!Files.exists(rootDirectory)) {
          Files.createDirectory(rootDirectory);
        }
        int j = 0;
        StringBuilder path = new StringBuilder();
        while (j < downloadFile.getPath().size() - 1) {
          path.append("/").append(downloadFile.getPath().get(j));
          Path filePath = Path.of(
              Constants.DOWNLOAD_ROOT_LOCATION + content.getRootDirectoryName() + path);
          if(!Files.exists(filePath)){
            Files.createDirectory(filePath);
          }
          j++;
        }
        randomAccessFileList[i] = new RandomAccessFile(
            Constants.DOWNLOAD_ROOT_LOCATION + content.getRootDirectoryName() + path + "/"
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
    try {
      lock.lock();
      log.info("Writing to disk: {}", downloadedBlock);

      // search the file to which this block belongs
      int fileIndex = contentMode == ContentMode.SINGLE ? 0
          : findFileIndexByPieceIndex(downloadedBlock.pieceIndex());

      RandomAccessFile file = randomAccessFileList[fileIndex];

      long offset = (downloadedBlock.pieceIndex() - startIndices[fileIndex]) * info.getPieceLength()
          + downloadedBlock.offset();

      file.seek(offset);

      file.write(downloadedBlock.data());
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void writeToDiskAsync(DownloadedBlock downloadedBlock) {
    this.executorService.execute(() -> {
      try {
        writeToDisk(downloadedBlock);
      } catch (IOException e) {
        log.error("Failed to write the downloaded block to disk!!");
        torrent.shutdown();
      }
    });
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

  @Override
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
