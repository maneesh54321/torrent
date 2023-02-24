package com.lib.torrent.test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SplitAndJoinFile {

  public static void main(String[] args) {

    String fileLocation = "/Users/maneesh/Downloads/ritika_aadhaar.pdf";
    int i = 0;
    try (FileInputStream fileInputStream = new FileInputStream(fileLocation)) {
      Files.createDirectory(Path.of("/Users/maneesh/Downloads/pieces"));
      byte[] chunk;

      while ((chunk = fileInputStream.readNBytes(2048)).length > 0) {
        String copyFile = "/Users/maneesh/Downloads/pieces/ritika_aadhaar_" + i++;
        try (FileOutputStream fos = new FileOutputStream(copyFile)) {
          fos.write(chunk);
        }
      }

    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    System.out.println("File split completed!!!");

    try (FileOutputStream finalFos = new FileOutputStream(
        "/Users/maneesh/Downloads/pieces/ritika_aadhaar_fiinal.pdf")) {
      int j = 0;
      while (j < i) {
        String fileToRead = "/Users/maneesh/Downloads/pieces/ritika_aadhaar_" + j++;
        try (FileInputStream fis = new FileInputStream(fileToRead)) {
          finalFos.write(fis.readAllBytes());
        }
      }
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    System.out.println("File joining completed!!!");
  }
}
