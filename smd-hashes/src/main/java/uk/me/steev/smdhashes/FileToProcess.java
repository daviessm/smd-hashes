package uk.me.steev.smdhashes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;

public class FileToProcess extends Task<Void> implements Comparable<FileToProcess> {
  private File file;
  private long size;
  private long bytesProcessedBefore;
  private final ReadOnlyLongWrapper readBytes = new ReadOnlyLongWrapper(0);
  private final ReadOnlyLongWrapper processedBytes = new ReadOnlyLongWrapper(0);
  private final ReadOnlyObjectWrapper<byte[]> checksumSHA256 = new ReadOnlyObjectWrapper<>();
  private final ReadOnlyObjectWrapper<byte[]> checksumMD5 = new ReadOnlyObjectWrapper<>();
  private final ReadOnlyObjectWrapper<Instant> startTime = new ReadOnlyObjectWrapper<>();
  private final ReadOnlyObjectWrapper<Instant> endTime = new ReadOnlyObjectWrapper<>();
  private final ReadOnlyStringWrapper blockedOn = new ReadOnlyStringWrapper();

  public FileToProcess(File file) {
    this.file = file;
    this.size = file.length();
  }

  @Override
  protected Void call() throws Exception {
    ExecutorService perFileExecutor = Executors.newFixedThreadPool(2);
    BlockingQueue<byte[]> bytesToProcess = new ArrayBlockingQueue<>(10);
    perFileExecutor.execute(() -> {
      System.out.println("Starting to read file " + file.getAbsolutePath());
      try (InputStream is = Files.newInputStream(file.toPath())) {
        this.startTime.set(Instant.now());
        byte[] buffer = new byte[1024*1024];
        long bytesRead = 0;
        while ((bytesRead = is.read(buffer)) > 0) {
          byte[] buffer2 = Arrays.copyOf(buffer, (int) bytesRead);
          bytesToProcess.put(buffer2);
          this.addToReadBytesProperty(bytesRead);
          this.blockedOn.set(bytesToProcess.remainingCapacity() < 5 ? "CPU" : "Disk");
        }
        bytesToProcess.put(new byte[0]);
      } catch (IOException | InterruptedException e) {
        e.printStackTrace();
      }
      System.out.println("Finished reading file " + file.getAbsolutePath());
    });

    perFileExecutor.execute(() -> {
      System.out.println("Starting to process file " + file.getAbsolutePath());
      try {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] buffer;
        while ((buffer = bytesToProcess.take()).length > 0) {
          md5.update(buffer);
          sha256.update(buffer);
          this.addToProcessedBytesProperty(buffer.length);
          this.updateProgress(this.processedBytes.get(), this.size);
          this.blockedOn.set(bytesToProcess.remainingCapacity() < 5 ? "CPU" : "Disk");
        }
        this.checksumMD5.set(md5.digest());
        this.checksumSHA256.set(sha256.digest());
        this.endTime.set(Instant.now());
      } catch (NoSuchAlgorithmException | InterruptedException e) {
        e.printStackTrace();
      }
      System.out.println("Finished processing file " + file.getAbsolutePath());
    });

    perFileExecutor.shutdown();
    try {
      perFileExecutor.awaitTermination(1, TimeUnit.DAYS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    this.blockedOn.set("Done");
    return null;
  }

  @Override
  public int compareTo(FileToProcess o) {
    if (this.size == o.getSize())
      return this.file.compareTo(o.getFile());
    if (this.size > o.getSize())
      return -1;
    return 1;
  }

  public void run() {
    try {
      call();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public File getFile() {
    return file;
  }

  public void setFile(File file) {
    this.file = file;
  }

  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }

  public ReadOnlyLongProperty readBytesProperty() {
    return readBytes.getReadOnlyProperty();
  }

  public void addToReadBytesProperty(long readBytes) {
    this.readBytes.set(this.readBytes.get() + readBytes);
  }

  public ReadOnlyLongProperty processedBytesProperty() {
    return processedBytes.getReadOnlyProperty();
  }

  public void addToProcessedBytesProperty(long processedBytes) {
    this.processedBytes.set(this.processedBytes.get() + processedBytes);
  }

  public ReadOnlyObjectProperty<byte[]> checksumSHA256Property() {
    return checksumSHA256.getReadOnlyProperty();
  }

  public void setChecksumSHA256Property(byte[] checksumSHA256) {
    this.checksumSHA256.set(checksumSHA256);
  }

  public ReadOnlyObjectProperty<byte[]> checksumMD5Property() {
    return checksumMD5.getReadOnlyProperty();
  }

  public void setChecksumMD5Property(byte[] checksumMD5) {
    this.checksumMD5.set(checksumMD5);
  }

  public ReadOnlyObjectProperty<Instant> startTimeProperty() {
    return startTime.getReadOnlyProperty();
  }

  public void setStartTimeProperty(Instant startTime) {
    this.startTime.set(startTime);
  }

  public ReadOnlyObjectProperty<Instant> endTimeProperty() {
    return endTime.getReadOnlyProperty();
  }

  public void setEndTimeProperty(Instant endTime) {
    this.endTime.set(endTime);
  }

  public ReadOnlyStringProperty blockedOnProperty() {
    return blockedOn.getReadOnlyProperty();
  }

  public void setEndTimeProperty(String blockedOn) {
    this.blockedOn.set(blockedOn);
  }

  @Override
  public String toString() {
    return "FileToProcess [file=" + file.getAbsolutePath() + ", size=" + size + ", readBytes=" + readBytes.get()
        + ", processedBytes=" + processedBytes.get() + ", checksumSHA256="
        + Utils.byteArrayToString(checksumSHA256.get(), false, false) + ", checksumMD5="
        + Utils.byteArrayToString(checksumMD5.get(), false, false) + ", startTime=" + startTime.get() + ", endTime="
        + endTime.get() + "]";
  }
}
