package uk.me.steev.smdhashes;

import java.io.File;

import javafx.application.Application;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class GUI extends Application {
  private File file;
  private Hashing hashing;

  @Override
  public void start(Stage stage) throws Exception {
    stage.setTitle("SMD Hashes");

    this.hashing = new Hashing();

    Scene scene = new Scene(new Group());
    stage.setTitle("SMD Hashes");
    stage.setWidth(1400);
    stage.setHeight(500);

    //Third: Table
    TableView<FileToProcess> table = new TableView<>();

    TableColumn<FileToProcess, String> fileColumn = new TableColumn<>("File path");
    fileColumn.prefWidthProperty().bind(table.widthProperty().add(-1150));
    fileColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(
        this.file.isFile() ? this.file.getName() : param.getValue().getFile().getPath().replace(this.file.getAbsolutePath(), "")));

    TableColumn<FileToProcess, Long> sizeColumn = new TableColumn<>("Size");
    sizeColumn.prefWidthProperty().set(80);
    sizeColumn.setCellValueFactory(new PropertyValueFactory<>("size"));
    sizeColumn.setCellFactory(tc -> new TableCell<FileToProcess, Long>() {
      @Override
      protected void updateItem(Long value, boolean empty) {
        super.updateItem(value, empty) ;
        if (empty) {
          setText(null);
        } else {
          setText(Utils.humanReadableByteCountBin(value));
        }
      }
    });

    TableColumn<FileToProcess, Long> processedColumn = new TableColumn<>("Processed");
    processedColumn.prefWidthProperty().set(80);
    processedColumn.setCellValueFactory(new PropertyValueFactory<FileToProcess,Long>("processedBytes"));
    processedColumn.setCellFactory(tc -> new TableCell<FileToProcess, Long>() {
      @Override
      protected void updateItem(Long value, boolean empty) {
          super.updateItem(value, empty) ;
          if (empty) {
              setText(null);
          } else {
              setText(Utils.humanReadableByteCountBin(value));
          }
      }
    });

    TableColumn<FileToProcess, String> blockedOnColumn = new TableColumn<>("Awaiting");
    blockedOnColumn.prefWidthProperty().set(80);
    blockedOnColumn.setCellValueFactory(new PropertyValueFactory<FileToProcess,String>("blockedOn"));

    TableColumn<FileToProcess, Double> progressColumn = new TableColumn<>("Progress");
    progressColumn.prefWidthProperty().set(150);
    progressColumn.setCellValueFactory(new PropertyValueFactory<FileToProcess,Double>("progress"));
    progressColumn.setCellFactory(ProgressBarTableCell.<FileToProcess> forTableColumn());

    TableColumn<FileToProcess, byte[]> md5Column = new TableColumn<>("MD5");
    md5Column.styleProperty().set("font-family: monospace;");
    md5Column.prefWidthProperty().set(340);
    md5Column.setCellValueFactory(new PropertyValueFactory<FileToProcess,byte[]>("checksumMD5"));
    md5Column.setCellFactory(tc -> new TableCell<FileToProcess, byte[]>() {
      @Override
      protected void updateItem(byte[] value, boolean empty) {
          super.updateItem(value, empty) ;
          if (empty) {
              setText(null);
          } else {
              setText(Utils.byteArrayToString(value, true, true));
              setFont(Font.font ("Courier New", FontWeight.BOLD, 11));
          }
      }
    });

    TableColumn<FileToProcess, byte[]> sha256Column = new TableColumn<>("SHA256");
    sha256Column.styleProperty().set("font-family: monospace;");
    sha256Column.prefWidthProperty().set(340);
    sha256Column.setCellValueFactory(new PropertyValueFactory<FileToProcess, byte[]>("checksumSHA256"));
    sha256Column.setCellFactory(tc -> new TableCell<FileToProcess, byte[]>() {
      @Override
      protected void updateItem(byte[] value, boolean empty) {
          super.updateItem(value, empty) ;
          if (empty) {
              setText(null);
          } else {
              setText(Utils.byteArrayToString(value, true, true));
              setFont(Font.font ("Courier New", FontWeight.BOLD, 11));
          }
      }
    });

    table.getColumns().setAll(fileColumn, sizeColumn, processedColumn, blockedOnColumn, progressColumn, md5Column, sha256Column);
    StackPane tablePane = new StackPane(table);

    //Top: title
    final Label title = new Label("SMD Hashes");
    title.setFont(new Font("Arial", 20));

    //Second: Select file/directory, number of threads,start button, status
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Select file");
    Button fileButton = new Button("File");
    fileButton.setOnAction(e -> {
        this.file = fileChooser.showOpenDialog(stage);
    });

    DirectoryChooser directoryChooser = new DirectoryChooser();
    directoryChooser.setTitle("Select Directory");
    Button directoryButton = new Button("Directory");
    directoryButton.setOnAction(e -> {
        this.file = directoryChooser.showDialog(stage);
    });

    Label processorsLabel = new Label("Threads");
    Slider processorsSlider = new Slider(1, Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors() - 1);
    processorsSlider.setMajorTickUnit(1);
    processorsSlider.setSnapToTicks(true);
    processorsSlider.setShowTickLabels(true);

    VBox processorsVbox = new VBox();
    processorsVbox.setPadding(new Insets(10, 10, 10, 10));
    processorsVbox.getChildren().addAll(processorsLabel, processorsSlider);

    Button startButton = new Button("Start");
    startButton.setOnAction(e -> {
      if (null == this.file) {
        Alert alert = new Alert(AlertType.ERROR, "No file or directory selected!", ButtonType.OK);
        alert.showAndWait();
      } else {
        this.hashing.setDirectory(this.file);
        this.hashing.setNumThreads((int) processorsSlider.getValue());
        this.hashing.setUpFiles();
        table.getItems().clear();
        for (FileToProcess f : hashing.getAllFiles()) {
          table.getItems().add(f);
        }
        //Platform.runLater(this.hashing);
        new Thread(this.hashing).start();
        fileButton.setDisable(true);
        directoryButton.setDisable(true);
        startButton.setDisable(true);
      }
    });

    final Text digestMD5 = new Text("Digest MD5");
    digestMD5.setFont(Font.font ("Courier New", FontWeight.BOLD, 11));
    digestMD5.textProperty().bind(this.hashing.digestMD5());

    final Text digestSHA256 = new Text("Digest SHA-256");
    digestSHA256.setFont(Font.font ("Courier New", FontWeight.BOLD, 11));
    digestSHA256.textProperty().bind(this.hashing.digestSHA256());

    VBox digestsVbox = new VBox();
    digestsVbox.setPadding(new Insets(10, 10, 10, 10));
    digestsVbox.getChildren().addAll(digestMD5, digestSHA256);

    HBox buttonsHbox = new HBox();
    buttonsHbox.setPadding(new Insets(10, 10, 10, 10));
    buttonsHbox.getChildren().addAll(fileButton, directoryButton, processorsVbox, startButton, digestsVbox);
    buttonsHbox.prefWidthProperty().bind(stage.widthProperty());

    final VBox vbox = new VBox();
    vbox.setSpacing(5);
    vbox.setPadding(new Insets(10, 0, 0, 10));
    vbox.getChildren().addAll(title, buttonsHbox, tablePane);
    vbox.prefHeightProperty().bind(stage.heightProperty());
    vbox.prefWidthProperty().bind(stage.widthProperty());
    table.prefHeightProperty().bind(vbox.heightProperty().add(-100));
    //table.prefWidthProperty().bind(stage.widthProperty());

    ((Group) scene.getRoot()).getChildren().addAll(vbox);

    stage.setScene(scene);
    stage.show();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
