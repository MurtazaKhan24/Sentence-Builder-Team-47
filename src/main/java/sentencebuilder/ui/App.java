/******************************************************************************
 * App.java
 *
 * Author: Zohaib
 * Revised by: James Human (JavaFX implementation)
 * Revised Date: 3/30/2026
 * Course: CS4485, Senior Design Project
 * This was made with the help of generative AI (Claude Code)
 *
 * Main JavaFX application entry point. Creates a tabbed interface with
 * File Import, Editor & Generator, Reports, and Admin views.
 ******************************************************************************/
package sentencebuilder.ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import sentencebuilder.algorithms.SentenceGenerator;
import sentencebuilder.db.*;
import sentencebuilder.db.model.*;
import sentencebuilder.parser.ImportService;

import java.io.File;
import java.util.List;

public class App extends Application {

    private DatabaseManager dbManager;
    private WordDao wordDao;
    private TransitionDao transitionDao;
    private FileDao fileDao;
    private OccurrenceDao occurrenceDao;
    private SentenceDao sentenceDao;
    private SentenceGenerator generator;
    private ImportService importService;

    @Override
    public void start(Stage primaryStage) {
        // Initialize database
        dbManager = new DatabaseManager("db.properties");
        wordDao = new WordDao(dbManager);
        transitionDao = new TransitionDao(dbManager);
        fileDao = new FileDao(dbManager);
        occurrenceDao = new OccurrenceDao(dbManager);
        sentenceDao = new SentenceDao(dbManager);
        generator = new SentenceGenerator(wordDao, transitionDao);
        importService = new ImportService(wordDao, transitionDao, fileDao, occurrenceDao);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        tabPane.getTabs().addAll(
            createImportTab(primaryStage),
            createGeneratorTab(),
            createReportsTab(),
            createAdminTab()
        );

        Scene scene = new Scene(tabPane, 800, 600);
        primaryStage.setTitle("Sentence Builder - Team 47");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Tab createImportTab(Stage stage) {
        Tab tab = new Tab("File Importer");
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.TOP_CENTER);

        Label title = new Label("Sentence Builder: File Importer");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        Label fileLabel = new Label("No file selected");
        Button selectButton = new Button("Select Text File (.txt)");
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(400);
        Label statusLabel = new Label("Ready");

        selectButton.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Text File");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"));
            File selectedFile = fileChooser.showOpenDialog(stage);

            if (selectedFile != null) {
                fileLabel.setText(selectedFile.getName());
                statusLabel.setText("Importing...");
                progressBar.setProgress(-1); // Indeterminate

                // Run import in background thread so UI doesn't freeze
                new Thread(() -> {
                    try {
                        int wordCount = importService.importFile(selectedFile);
                        javafx.application.Platform.runLater(() -> {
                            progressBar.setProgress(1.0);
                            if (wordCount == -1) {
                                statusLabel.setText("File already imported!");
                            } else {
                                statusLabel.setText("Done! Imported " + wordCount + " unique words.");
                            }
                        });
                    } catch (Exception importException) {
                        javafx.application.Platform.runLater(() -> {
                            progressBar.setProgress(0);
                            statusLabel.setText("Error: " + importException.getMessage());
                        });
                    }
                }).start();
            }
        });

        layout.getChildren().addAll(title, selectButton, fileLabel,
            new Label("Import Progress"), progressBar, statusLabel);
        tab.setContent(layout);
        return tab;
    }

    private Tab createGeneratorTab() {
        Tab tab = new Tab("Editor & Generator");
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));

        Label title = new Label("Sentence Builder: Editor & Generator");
        title.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        // Manual editor with auto-complete
        Label editorLabel = new Label("1. Manual Editor (Auto-complete triggers on space):");
        TextArea editorArea = new TextArea();
        editorArea.setPrefRowCount(5);

        Label suggestionsLabel = new Label("Suggestions:");
        TextField suggestionsField = new TextField();
        suggestionsField.setEditable(false);

        // Auto-complete: when user types a space, suggest next words
        editorArea.textProperty().addListener((obs, oldText, newText) -> {
            if (newText.endsWith(" ")) {
                String[] words = newText.trim().split("\\s+");
                if (words.length > 0) {
                    String lastWord = words[words.length - 1].toLowerCase();
                    Word dbWord = wordDao.findByWord(lastWord);
                    if (dbWord != null) {
                        List<Transition> nextOptions = transitionDao.findByWordIdWeighted(dbWord.getWordId());
                        StringBuilder suggestions = new StringBuilder();
                        int limit = Math.min(5, nextOptions.size());
                        for (int ix = 0; ix < limit; ix++) {
                            Word nextWord = wordDao.findById(nextOptions.get(ix).getNextWordId());
                            if (nextWord != null) {
                                if (suggestions.length() > 0) suggestions.append(", ");
                                suggestions.append(nextWord.getWord());
                            }
                        }
                        suggestionsField.setText(suggestions.toString());
                    }
                }
            }
        });

        // Sentence generator section
        Label genLabel = new Label("2. Sentence Generator:");
        HBox genControls = new HBox(10);
        genControls.setAlignment(Pos.CENTER_LEFT);
        TextField seedField = new TextField();
        seedField.setPromptText("Enter starting word...");
        ComboBox<String> algoBox = new ComboBox<>();
        algoBox.getItems().addAll("WEIGHTED_RANDOM", "MOST_FREQUENT");
        algoBox.setValue("WEIGHTED_RANDOM");
        Button generateButton = new Button("Generate");
        genControls.getChildren().addAll(seedField, algoBox, generateButton);

        Label resultLabel = new Label("[Generated sentence will appear here]");
        resultLabel.setWrapText(true);
        resultLabel.setStyle("-fx-text-fill: green; -fx-font-size: 14;");

        generateButton.setOnAction(event -> {
            String seed = seedField.getText().trim();
            if (!seed.isEmpty()) {
                String sentence = generator.generate(seed, 15, algoBox.getValue());
                resultLabel.setText(sentence);

                // Save to DB
                Word seedWordObj = wordDao.findByWord(seed.toLowerCase());
                Integer seedWordId = seedWordObj != null ? seedWordObj.getWordId() : null;
                String[] generatedWords = sentence.split("\\s+");
                sentenceDao.insert(new GeneratedSentence(
                    sentence, seedWordId, algoBox.getValue(), generatedWords.length));
            }
        });

        layout.getChildren().addAll(title, editorLabel, editorArea,
            suggestionsLabel, suggestionsField, genLabel, genControls, resultLabel);
        tab.setContent(layout);
        return tab;
    }

    private Tab createReportsTab() {
        Tab tab = new Tab("Reports");
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));

        Label title = new Label("Sentence Builder: Reports & History");
        title.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        TabPane innerTabs = new TabPane();
        innerTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Word Statistics tab
        Tab wordStatsTab = new Tab("Word Statistics");
        TableView<Word> wordTable = new TableView<>();
        TableColumn<Word, String> wordCol = new TableColumn<>("Word");
        wordCol.setCellValueFactory(new PropertyValueFactory<>("word"));
        TableColumn<Word, Integer> freqCol = new TableColumn<>("Frequency");
        freqCol.setCellValueFactory(new PropertyValueFactory<>("totalCount"));
        TableColumn<Word, Integer> startCol = new TableColumn<>("Start Count");
        startCol.setCellValueFactory(new PropertyValueFactory<>("startCount"));
        TableColumn<Word, Integer> endCol = new TableColumn<>("End Count");
        endCol.setCellValueFactory(new PropertyValueFactory<>("endCount"));
        wordTable.getColumns().addAll(wordCol, freqCol, startCol, endCol);

        Button refreshWords = new Button("Refresh");
        refreshWords.setOnAction(event -> {
            wordTable.getItems().clear();
            wordTable.getItems().addAll(wordDao.getTopN(100));
        });

        VBox wordStatsLayout = new VBox(10, refreshWords, wordTable);
        wordStatsLayout.setPadding(new Insets(10));
        wordStatsTab.setContent(wordStatsLayout);

        // Generation History tab
        Tab historyTab = new Tab("Generation History");
        TableView<GeneratedSentence> historyTable = new TableView<>();
        TableColumn<GeneratedSentence, String> sentenceCol = new TableColumn<>("Sentence");
        sentenceCol.setCellValueFactory(new PropertyValueFactory<>("sentenceText"));
        sentenceCol.setPrefWidth(400);
        TableColumn<GeneratedSentence, String> algoCol = new TableColumn<>("Algorithm");
        algoCol.setCellValueFactory(new PropertyValueFactory<>("algorithm"));
        TableColumn<GeneratedSentence, Integer> wcCol = new TableColumn<>("Words");
        wcCol.setCellValueFactory(new PropertyValueFactory<>("wordCount"));
        historyTable.getColumns().addAll(sentenceCol, algoCol, wcCol);

        Button refreshHistory = new Button("Refresh");
        refreshHistory.setOnAction(event -> {
            historyTable.getItems().clear();
            historyTable.getItems().addAll(sentenceDao.findAll());
        });

        VBox historyLayout = new VBox(10, refreshHistory, historyTable);
        historyLayout.setPadding(new Insets(10));
        historyTab.setContent(historyLayout);

        innerTabs.getTabs().addAll(wordStatsTab, historyTab);
        layout.getChildren().addAll(title, innerTabs);
        tab.setContent(layout);
        return tab;
    }

    private Tab createAdminTab() {
        Tab tab = new Tab("Admin");
        HBox layout = new HBox(20);
        layout.setPadding(new Insets(20));

        // Word list on left
        VBox leftPane = new VBox(10);
        Label listLabel = new Label("Word List");
        listLabel.setStyle("-fx-font-weight: bold;");
        ListView<String> wordListView = new ListView<>();
        Button refreshList = new Button("Refresh");
        refreshList.setOnAction(event -> {
            wordListView.getItems().clear();
            List<Word> allWords = wordDao.findAll();
            for (Word wordItem : allWords) {
                wordListView.getItems().add(wordItem.getWord());
            }
        });
        leftPane.getChildren().addAll(listLabel, refreshList, wordListView);

        // Edit form on right
        VBox rightPane = new VBox(10);
        Label editLabel = new Label("Edit Word Metadata");
        editLabel.setStyle("-fx-font-weight: bold;");
        TextField selectedWordField = new TextField();
        selectedWordField.setPromptText("Selected Word");
        selectedWordField.setEditable(false);
        TextField freqField = new TextField();
        freqField.setPromptText("Total Frequency");
        TextField startsField = new TextField();
        startsField.setPromptText("Sentence Starts");
        TextField endsField = new TextField();
        endsField.setPromptText("Sentence Ends");
        Button saveButton = new Button("Save Changes");
        saveButton.setStyle("-fx-background-color: #2d7d46; -fx-text-fill: white;");
        Label statusLabel = new Label("");

        // When a word is selected, populate the form
        wordListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal != null) {
                    Word wordItem = wordDao.findByWord(newVal);
                    if (wordItem != null) {
                        selectedWordField.setText(wordItem.getWord());
                        freqField.setText(String.valueOf(wordItem.getTotalCount()));
                        startsField.setText(String.valueOf(wordItem.getStartCount()));
                        endsField.setText(String.valueOf(wordItem.getEndCount()));
                        statusLabel.setText("Editing: " + wordItem.getWord());
                    }
                }
            });

        saveButton.setOnAction(event -> {
            String selectedWord = selectedWordField.getText();
            if (!selectedWord.isEmpty()) {
                Word wordItem = wordDao.findByWord(selectedWord);
                if (wordItem != null) {
                    wordItem.setTotalCount(Integer.parseInt(freqField.getText()));
                    wordItem.setStartCount(Integer.parseInt(startsField.getText()));
                    wordItem.setEndCount(Integer.parseInt(endsField.getText()));
                    wordDao.update(wordItem);
                    statusLabel.setText("Saved: " + selectedWord);
                }
            }
        });

        rightPane.getChildren().addAll(editLabel,
            new Label("Selected Word:"), selectedWordField,
            new Label("Total Frequency:"), freqField,
            new Label("Sentence Starts:"), startsField,
            new Label("Sentence Ends:"), endsField,
            saveButton, statusLabel);

        layout.getChildren().addAll(leftPane, rightPane);
        tab.setContent(layout);
        return tab;
    }

    @Override
    public void stop() {
        if (dbManager != null) {
            dbManager.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
