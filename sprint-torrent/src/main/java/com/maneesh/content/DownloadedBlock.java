package com.maneesh.content;

public record DownloadedBlock(int pieceIndex, int offset, byte[] data) {

}
