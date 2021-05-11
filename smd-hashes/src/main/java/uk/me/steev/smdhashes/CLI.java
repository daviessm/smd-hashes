package uk.me.steev.smdhashes;

import java.io.File;

import javafx.application.Application;
import javafx.stage.Stage;

public class CLI extends Application {
  private Hashing hashing;
  private File file;

  public CLI() {
    this.hashing = new Hashing();
  }

  public void init() {
    this.file = new File(getParameters().getRaw().get(0));
  }

  public void setFile(String filePath) {
    this.file = new File (filePath);
  }

  @Override
  public void start(Stage stage) throws Exception {
    this.hashing.setDirectory(this.file);
    this.hashing.setNumThreads(3);
    this.hashing.setUpFiles();
    this.hashing.call();
    System.out.println(this.hashing.digestSHA256());
  }

  public static void main(String[] args) {
    Application.launch(CLI.class, args);
  }
}
