/******************************************************************************
 * App.java
 *
 * Author: Zohaib Saqib
 * Revised by: James Human (JavaFX implementation) and Murtaza Khan (refactoring and feature additions)
 * Revised Dates: 3/30/2026, 4/29/2026
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
import sentencebuilder.utils.SpellChecker;

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
    private SpellChecker spellChecker;

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
            spellChecker = new SpellChecker(wordDao);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setTabMinWidth(150);
        tabPane.setTabMaxWidth(300);

        tabPane.getTabs().addAll(
            createImportTab(primaryStage),
            createGeneratorTab(),
            createReportsTab(),
            createAdminTab()
        );

        Scene scene = new Scene(tabPane, 800, 600);
        scene.getRoot().setStyle(
            "-fx-base: #F5D5DC; "
            + "-fx-background: #FDF2F4; "
            + "-fx-control-inner-background: #FFFAFB; "
            + "-fx-accent: #E8A0B4; "
            + "-fx-focus-color: #D4839B; "
            + "-fx-faint-focus-color: #D4839B22;"
        );

        // Make tabs stretch to fill the full width
        tabPane.tabMinWidthProperty().bind(
            tabPane.widthProperty().divide(tabPane.getTabs().size()).subtract(20));

        tabPane.setStyle("-fx-tab-min-height: 30; "
            + "-fx-base: #F5D5DC; "
            + "-fx-background: #FDF2F4;");
        primaryStage.setTitle("Sentence Builder - Team 47");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Tab createImportTab(Stage stage) {
        Tab tab = new Tab("File Importer");
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));

        Label title = new Label("Sentence Builder: File Importer");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        // Import controls row
        HBox importRow = new HBox(10);
        importRow.setAlignment(Pos.CENTER_LEFT);
        Label fileLabel = new Label("No file selected");
        Button selectButton = new Button("Select Text File (.txt)");
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(200);
        Label statusLabel = new Label("Ready");
        importRow.getChildren().addAll(selectButton, fileLabel, progressBar, statusLabel);

        // Previously imported files table
        Label historyLabel = new Label("Imported Files");
        historyLabel.setStyle("-fx-font-weight: bold;");
        TableView<ImportedFile> fileTable = new TableView<>();
        fileTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<ImportedFile, String> nameCol = new TableColumn<>("Filename");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("filename"));
        TableColumn<ImportedFile, String> pathCol = new TableColumn<>("Path");
        pathCol.setCellValueFactory(new PropertyValueFactory<>("filepath"));
        TableColumn<ImportedFile, Integer> wcCol = new TableColumn<>("Word Count");
        wcCol.setCellValueFactory(new PropertyValueFactory<>("wordCount"));
        fileTable.getColumns().addAll(nameCol, pathCol, wcCol);
        VBox.setVgrow(fileTable, javafx.scene.layout.Priority.ALWAYS);

        // Load existing files on tab creation
        fileTable.getItems().addAll(fileDao.findAll());

        selectButton.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Text File");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"));
            File selectedFile = fileChooser.showOpenDialog(stage);

            if (selectedFile != null) {
                fileLabel.setText(selectedFile.getName());
                statusLabel.setText("Importing...");
                progressBar.setProgress(-1);

                new Thread(() -> {
                    try {
                        int wordCount = importService.importFile(selectedFile);
                        javafx.application.Platform.runLater(() -> {
                            progressBar.setProgress(1.0);
                            if (wordCount == -1) {
                                statusLabel.setText("File already imported!");
                            } else {
                                statusLabel.setText("Done! " + wordCount + " unique words.");
                            }
                            // Refresh file list
                            fileTable.getItems().clear();
                            fileTable.getItems().addAll(fileDao.findAll());
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

        layout.getChildren().addAll(title, importRow, historyLabel, fileTable);
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
        suggestionsField.setPrefWidth(520);
        Button addWordButton = new Button("Add Word");
        addWordButton.setVisible(false);

        // Place suggestions field and Add button on the same row so the
        // button is visible when offered.
        HBox suggestionsRow = new HBox(8, suggestionsField, addWordButton);
        suggestionsRow.setAlignment(Pos.CENTER_LEFT);

        // Keep last typed word accessible for the Add button
        final String[] lastWordRef = new String[1];

        addWordButton.setOnAction(evt -> {
            String candidate = lastWordRef[0];
            if (candidate == null || candidate.isBlank()) return;
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Add Word");
            confirm.setHeaderText(null);
            confirm.setContentText("Add \"" + candidate + "\" to the database as a user-added word?");
            java.util.Optional<javafx.scene.control.ButtonType> resp = confirm.showAndWait();
            if (resp.isPresent() && resp.get() == javafx.scene.control.ButtonType.OK) {
                Word newWord = new Word(candidate, 0, 0, 0);
                newWord.setUserAdded(true);
                int newId = wordDao.insert(newWord);
                if (newId != -1) {
                    suggestionsField.setText("Added: " + candidate);
                    addWordButton.setVisible(false);
                } else {
                    suggestionsField.setText("Failed to add: " + candidate);
                }
            }
        });

        // Auto-complete: when user types a space or comma, suggest next words
        editorArea.textProperty().addListener((obs, oldText, newText) -> {
            if (newText.endsWith(" ") || newText.endsWith(",")) {
                String[] words = newText.trim().split("[\\s,]+");
                if (words.length > 0) {
                    String lastWord = words[words.length - 1]
                        .replaceAll("^[^a-zA-Z0-9']+|[^a-zA-Z0-9']+$", "")
                        .toLowerCase();
                    if (lastWord.isBlank()) {
                        suggestionsField.clear();
                        addWordButton.setVisible(false);
                        return;
                    }
                    lastWordRef[0] = lastWord;
                    Word dbWord = wordDao.findByWord(lastWord);
                    StringBuilder suggestions = new StringBuilder();

                    if (dbWord != null) {
                        List<Transition> nextOptions = transitionDao.findByWordIdWeighted(dbWord.getWordId());
                        if (!nextOptions.isEmpty()) {
                            int limit = Math.min(5, nextOptions.size());
                            for (int ix = 0; ix < limit; ix++) {
                                Word nextWord = wordDao.findById(nextOptions.get(ix).getNextWordId());
                                if (nextWord != null) {
                                    if (suggestions.length() > 0) suggestions.append(", ");
                                    suggestions.append(nextWord.getWord());
                                }
                            }
                        } else {
                            // No transitions for this corpus word: fallback to top words
                            List<Word> topWords = wordDao.getTopN(5);
                            for (Word topWord : topWords) {
                                if (suggestions.length() > 0) suggestions.append(", ");
                                suggestions.append(topWord.getWord());
                            }
                            // word exists but has no transitions — do not offer Add
                            addWordButton.setVisible(false);
                        }
                    } else {
                        // Word not found in corpus — treat as possible misspelling
                        List<String> spellSuggestions = spellChecker.getSuggestions(lastWord);
                        if (!spellSuggestions.isEmpty()) {
                            suggestions.append("Did you mean: ");
                            suggestions.append(String.join(", ", spellSuggestions));
                            // Offer to add unknown word
                            addWordButton.setVisible(true);
                        } else {
                            // No near matches — show top words as fallback
                            List<Word> topWords = wordDao.getTopN(5);
                            for (Word topWord : topWords) {
                                if (suggestions.length() > 0) suggestions.append(", ");
                                suggestions.append(topWord.getWord());
                            }
                            addWordButton.setVisible(true);
                        }
                    }

                    suggestionsField.setText(suggestions.toString());
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
        algoBox.getItems().addAll("WEIGHTED_RANDOM", "MOST_FREQUENT", "MOST_FREQUENT_RANDOM");
        algoBox.setValue("WEIGHTED_RANDOM");
        Button generateButton = new Button("Generate");
        genControls.getChildren().addAll(seedField, algoBox, generateButton);

        Label resultLabel = new Label("[Generated sentence will appear here]");
        resultLabel.setWrapText(true);
        resultLabel.setStyle("-fx-text-fill: #C71585; -fx-font-size: 14;");

            Label spellCheckLabel = new Label("");
            spellCheckLabel.setWrapText(true);
            spellCheckLabel.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 12;");

        generateButton.setOnAction(event -> {
            String seed = seedField.getText().trim();
            if (!seed.isEmpty()) {
                String seedLower = seed.toLowerCase();

                // Check spelling
                spellCheckLabel.setText("");
                if (spellChecker.isMisspelled(seedLower)) {
                    List<String> suggestions = spellChecker.getSuggestions(seedLower);
                    if (!suggestions.isEmpty()) {
                        spellCheckLabel.setText("⚠ Possible misspelling. Did you mean: " + String.join(", ", suggestions) + "?");
                    } else {
                        spellCheckLabel.setText("⚠ Word not found in corpus.");
                    }
                }

                // If the seed word isn't present in the DB, ask the user whether
                // to add it as a user-added word.
                Word seedWordObj = wordDao.findByWord(seedLower);
                if (seedWordObj == null) {
                    Alert askAdd = new Alert(Alert.AlertType.CONFIRMATION);
                    askAdd.setTitle("Add Word");
                    askAdd.setHeaderText(null);
                    askAdd.setContentText("The word \"" + seed + "\" is not in the database. Add it?");
                    java.util.Optional<javafx.scene.control.ButtonType> resp = askAdd.showAndWait();
                    if (resp.isPresent() && resp.get() == javafx.scene.control.ButtonType.OK) {
                        Word newWord = new Word(seedLower, 0, 0, 0);
                        newWord.setUserAdded(true);
                        int newId = wordDao.insert(newWord);
                        newWord.setWordId(newId);
                        seedWordObj = newWord;
                    }
                }

                String sentence = generator.generate(seedLower, 15, algoBox.getValue());
                resultLabel.setText(sentence);

                // Save to DB (record generated sentence). If seed word was added above
                // use its id; otherwise save null for seed id when unknown.
                Integer seedWordId = seedWordObj != null ? seedWordObj.getWordId() : null;
                String[] generatedWords = sentence.split("\\s+");
                sentenceDao.insert(new GeneratedSentence(
                    sentence, seedWordId, algoBox.getValue(), generatedWords.length));
            }
        });

        layout.getChildren().addAll(title, editorLabel, editorArea,
            suggestionsLabel, suggestionsRow, genLabel, genControls, resultLabel);
        layout.getChildren().add(spellCheckLabel);
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
        wordTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<Word, String> wordCol = new TableColumn<>("Word");
        wordCol.setCellValueFactory(new PropertyValueFactory<>("word"));
        TableColumn<Word, Integer> freqCol = new TableColumn<>("Frequency");
        freqCol.setCellValueFactory(new PropertyValueFactory<>("totalCount"));
        TableColumn<Word, Integer> startCol = new TableColumn<>("Start Count");
        startCol.setCellValueFactory(new PropertyValueFactory<>("startCount"));
        TableColumn<Word, Integer> endCol = new TableColumn<>("End Count");
        endCol.setCellValueFactory(new PropertyValueFactory<>("endCount"));
        wordTable.getColumns().addAll(wordCol, freqCol, startCol, endCol);

        // Search controls for Word Statistics
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        TextField searchField = new TextField();
        searchField.setPromptText("Search words...");
        searchField.setPrefWidth(200);
        Button searchButton = new Button("Search");
        Button refreshWords = new Button("Refresh");
        ComboBox<String> sortBox = new ComboBox<>();
        sortBox.getItems().addAll("Alphabetical", "Frequency", "Start Count", "End Count");
        sortBox.setValue("Alphabetical");

        Runnable refreshWordTable = () -> {
            String query = searchField.getText().trim();
            java.util.List<Word> words = new java.util.ArrayList<>();
            if (!query.isEmpty()) {
                words.addAll(wordDao.findByPrefix(query, 1000));
            } else {
                words.addAll(wordDao.findAll());
            }

            String sortMode = sortBox.getValue();
            if ("Frequency".equals(sortMode)) {
                words.sort(java.util.Comparator.comparingInt(Word::getTotalCount).reversed()
                    .thenComparing(Word::getWord));
            } else if ("Start Count".equals(sortMode)) {
                words.sort(java.util.Comparator.comparingInt(Word::getStartCount).reversed()
                    .thenComparing(Word::getWord));
            } else if ("End Count".equals(sortMode)) {
                words.sort(java.util.Comparator.comparingInt(Word::getEndCount).reversed()
                    .thenComparing(Word::getWord));
            } else {
                words.sort(java.util.Comparator.comparing(Word::getWord));
            }

            wordTable.getItems().setAll(words);
        };
        
        searchButton.setOnAction(event -> refreshWordTable.run());
        
        refreshWords.setOnAction(event -> {
            sortBox.setValue("Alphabetical");
            searchField.clear();
            refreshWordTable.run();
        });
        
        sortBox.setOnAction(event -> refreshWordTable.run());
        searchBox.getChildren().addAll(new Label("Find:"), searchField, searchButton,
            new Label("Sort:"), sortBox, refreshWords);

        VBox wordStatsLayout = new VBox(10, searchBox, wordTable);
        wordStatsLayout.setPadding(new Insets(10));
        wordStatsTab.setContent(wordStatsLayout);

        // Generation History tab
        Tab historyTab = new Tab("Generation History");
        TableView<GeneratedSentence> historyTable = new TableView<>();
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<GeneratedSentence, String> sentenceCol = new TableColumn<>("Sentence");
        sentenceCol.setCellValueFactory(new PropertyValueFactory<>("sentenceText"));
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

        // Duplicate sentence report
        Tab duplicateTab = new Tab("Duplicates");
        TableView<SentenceDuplicate> duplicateTable = new TableView<>();
        duplicateTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<SentenceDuplicate, String> duplicateSentenceCol = new TableColumn<>("Sentence");
        duplicateSentenceCol.setCellValueFactory(new PropertyValueFactory<>("sentenceText"));
        TableColumn<SentenceDuplicate, Integer> duplicateCountCol = new TableColumn<>("Occurrences");
        duplicateCountCol.setCellValueFactory(new PropertyValueFactory<>("duplicateCount"));
        duplicateTable.getColumns().addAll(duplicateSentenceCol, duplicateCountCol);

        Button refreshDuplicates = new Button("Refresh");
        refreshDuplicates.setOnAction(event -> {
            duplicateTable.getItems().setAll(sentenceDao.findDuplicates());
        });

        VBox duplicateLayout = new VBox(10, refreshDuplicates, duplicateTable);
        duplicateLayout.setPadding(new Insets(10));
        duplicateTab.setContent(duplicateLayout);

        innerTabs.getTabs().addAll(wordStatsTab, historyTab, duplicateTab);
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
        
        // Search controls for Admin word list
        HBox adminSearchBox = new HBox(10);
        adminSearchBox.setAlignment(Pos.CENTER_LEFT);
        TextField adminSearchField = new TextField();
        adminSearchField.setPromptText("Search words...");
        adminSearchField.setPrefWidth(150);
        Button adminSearchButton = new Button("Search");
        Button refreshList = new Button("Refresh");
        
        adminSearchButton.setOnAction(event -> {
            String query = adminSearchField.getText().trim();
            wordListView.getItems().clear();
            if (!query.isEmpty()) {
                List<Word> results = wordDao.findByPrefix(query, 200);
                for (Word wordItem : results) {
                    wordListView.getItems().add(wordItem.getWord());
                }
            } else {
                List<Word> allWords = wordDao.findAll();
                for (Word wordItem : allWords) {
                    wordListView.getItems().add(wordItem.getWord());
                }
            }
        });
        
        refreshList.setOnAction(event -> {
            wordListView.getItems().clear();
            List<Word> allWords = wordDao.findAll();
            for (Word wordItem : allWords) {
                wordListView.getItems().add(wordItem.getWord());
            }
            adminSearchField.clear();
        });
        
        adminSearchBox.getChildren().addAll(adminSearchButton, adminSearchField, refreshList);
        leftPane.getChildren().addAll(listLabel, adminSearchBox, wordListView);

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
        saveButton.setStyle("-fx-background-color: #D4839B; -fx-text-fill: white;");
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
