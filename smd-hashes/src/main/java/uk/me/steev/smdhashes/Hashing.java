package uk.me.steev.smdhashes;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;

public class Hashing extends Task<Void> implements Runnable {
  private File directory;
  private int numThreads = 2;
  private SortedSet<FileToProcess> allFiles;
  private Instant startTime;
  private Instant endTime;
  private ReadOnlyStringWrapper elapsedTime;
  private ReadOnlyStringWrapper digestMD5 = new ReadOnlyStringWrapper();
  private ReadOnlyStringWrapper digestSHA256 = new ReadOnlyStringWrapper();

  public Hashing() {
    super();
  }

  public void setUpFiles() {
    this.allFiles = addEntryToList(new TreeSet<FileToProcess>(), this.directory);
  }

  public Void call() {
    this.startTime = Instant.now();
    this.digestMD5.set("MD5: Calculating...");
    this.digestSHA256.set("SHA-256: Calculating...");
    ExecutorService mainExecutor = Executors.newFixedThreadPool(numThreads);
    this.allFiles.forEach(file -> {
      mainExecutor.submit(file);
    });
    mainExecutor.shutdown();
    try {
      mainExecutor.awaitTermination(1, TimeUnit.HOURS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    try {
      MessageDigest md5 = MessageDigest.getInstance("MD5");
      MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
      allFiles.forEach(file -> {
        md5.update(file.checksumMD5Property().get());
        sha256.update(file.checksumSHA256Property().get());
      });
      this.digestMD5.set("MD5: " + Utils.byteArrayToString(md5.digest(), true, true));
      this.digestSHA256.set("SHA-256: " + Utils.byteArrayToString(sha256.digest(), true, true));
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    this.endTime = Instant.now();

    return null;
  }

  private static SortedSet<FileToProcess> addEntryToList(SortedSet<FileToProcess> files, File file) {
    if (file.isDirectory()) {
      for (File f : file.listFiles()) {
        files.addAll(addEntryToList(files, f));
      }
    } else {
      files.add(new FileToProcess(file));
    }
    return files;
  }

  public File getDirectory() {
    return directory;
  }

  public int getNumThreads() {
    return numThreads;
  }

  public void setNumThreads(int numThreads) {
    this.numThreads = numThreads;
  }

  public void setDirectory(File directory) {
    this.directory = directory;
  }

  public SortedSet<FileToProcess> getAllFiles() {
    return allFiles;
  }

  public void setAllFiles(SortedSet<FileToProcess> allFiles) {
    this.allFiles = allFiles;
  }

  public ReadOnlyStringWrapper digestMD5() {
    return digestMD5;
  }

  public ReadOnlyStringWrapper digestSHA256() {
    return digestSHA256;
  }
}
