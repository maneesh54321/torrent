package com.maneesh.piece;

import com.maneesh.content.DownloadedBlock;
import com.maneesh.core.Peer;
import com.maneesh.network.message.IMessage;
import java.util.Collection;

public interface PieceDownloadScheduler {
  
  void completeBlockDownload(DownloadedBlock downloadedBlock);

  void failBlocksDownload(Collection<IMessage> messages, Peer peer);
}
