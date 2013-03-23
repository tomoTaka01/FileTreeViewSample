package filetreeviewsample;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class FileTreeViewSample extends Application {

    private ExecutorService service;
    private Task watchTask;
    private SearchTask searchTask;
    private TextField rootDirText;
    private Path rootPath;
    private Button dispBtn;
    private TreeView<PathItem> fileTreeView;
    private CheckBox watchChkbox;
    private TextArea watchText;
    private TextField patternText;
    private Button searchBtn;
    private ListView<String> searchListView;
    private ObservableList<String> searchListItem;
    private StringProperty searchResult = new SimpleStringProperty();
    private List<String> searchList = new ArrayList<>();
    private Label searchCountLabel;

    public FileTreeViewSample() {
        fileTreeView = new TreeView<>();
        service = Executors.newFixedThreadPool(2);
    }

    @Override
    public void start(Stage stage) {
        VBox root = new VBox();
        root.setPadding(new Insets(10));
        root.setSpacing(10);
        // root Directory
        HBox rootHbox = getRootHbox(stage);
        TitledPane titledPane = new TitledPane("File Tree View", fileTreeView);
        titledPane.setPrefHeight(300);
        // Watch Directory
        watchChkbox = new CheckBox("Watch Directory");
        watchChkbox.setDisable(true);
        watchText = new TextArea(); // watch result
        watchText.setPrefHeight(50);
        // search
        HBox searchHbox = getSearchHbox();
        searchListView = new ListView<>(); // search result
        searchListView.setPrefHeight(100);
        searchListItem = FXCollections.observableArrayList();
        searchListView.setItems(searchListItem);
        setEventHandler();
        root.getChildren().addAll(rootHbox, titledPane, watchChkbox, watchText, searchHbox, searchListView);
        Scene scene = new Scene(root, 800, 600);
        stage.setTitle("File Tree View Sample");
        stage.setScene(scene);
        stage.show();
    }

    private HBox getRootHbox(final Stage stage) {
        HBox hbox = new HBox();
        hbox.setSpacing(10);
        Label label = new Label("root Directory:");
        rootDirText = new TextField();
        rootDirText.setPrefWidth(300);
        rootDirText.requestFocus(); // this does not work?
        final DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("select Directory");
        Image image = new Image(getClass().getResourceAsStream("OpenDirectory.png"));
        Button chooserBtn = new Button("", new ImageView(image));
        chooser.setTitle("Select Root Directory");
        chooserBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                File selDir = chooser.showDialog(stage);
                if (selDir != null) {
                    rootDirText.setText(selDir.getAbsolutePath());
                }
            }
        });
        dispBtn = new Button("Display File Tree");
        hbox.getChildren().addAll(label, rootDirText, chooserBtn, dispBtn);
        return hbox;
    }

    private HBox getSearchHbox() {
        HBox hBox = new HBox();
        hBox.setSpacing(10);
        Label searchLabel = new Label("search:");
        patternText = new TextField();
        Image image = new Image(getClass().getResourceAsStream("Search.png"));
        searchBtn = new Button("", new ImageView(image));
        searchBtn.setDisable(true);
        searchCountLabel = new Label();
        searchCountLabel.setPrefWidth(200);
        searchCountLabel.setAlignment(Pos.CENTER_LEFT);
        hBox.getChildren().addAll(searchLabel, patternText, searchBtn, searchCountLabel);
        return hBox;
    }

    private void setEventHandler() {
        // Display File Tree Button
        dispBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                watchChkbox.setDisable(false);
                watchChkbox.setSelected(false);
                watchText.clear();
                searchBtn.setDisable(false);
                searchListItem.clear();
                searchResult.unbind();
                searchCountLabel.textProperty().unbind();
                searchCountLabel.setText(null);
                if (watchTask != null && watchTask.isRunning()) {
                    watchTask.cancel();
                }
                if (searchTask != null && searchTask.isRunning()) {
                    searchTask.cancel();
                }
                rootPath = Paths.get(rootDirText.getText());
                PathItem pathItem = new PathItem(rootPath);
                fileTreeView.setRoot(createNode(pathItem));
            }
        });
        // Watch Directory CheckBox
        watchChkbox.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean oldVal, Boolean newVal) {
                if (newVal) {
                    watchTask = new WatchTask(rootPath);
                    service.submit(watchTask);
                    watchText.textProperty().bind(watchTask.messageProperty());
                } else {
                    if (watchTask != null && watchTask.isRunning()) {
                        watchTask.cancel();
                        watchText.textProperty().unbind();
                    }
                }
            }
        });
        // search button
        searchBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                searchListItem.clear();                
                searchTask = new SearchTask(rootPath, patternText.getText());
                searchCountLabel.textProperty().bind(searchTask.messageProperty());
                searchList.clear();
                searchResult.bind(searchTask.getResultString());
                searchTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                    @Override
                    public void handle(WorkerStateEvent t) {
                        searchListItem.addAll(searchList); // add all search result at once
                    }
                });
                // this bind does not work???
//                searchCountLabel.textProperty().bind(
//                        new StringBinding() {
//                    {
//                        bind(searchTask.progressProperty());
//                    }
//                    @Override
//                    protected String computeValue() {
//                        return String.format("%1.0f files founded", searchTask.progressProperty().getValue());
//                    }
//                });
                service.submit(searchTask);
            }
        });
        // *** binding to search task result one by one ***
        searchResult.addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> ov, String oldVal, String newVal) {
                if (newVal != null) {
                    searchList.add(newVal);
                }
            }
        });
    }

    private TreeItem<PathItem> createNode(PathItem pathItem) {
        return PathTreeItem.createNode(pathItem);
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void stop() throws Exception {
        service.shutdownNow();
    }
}
