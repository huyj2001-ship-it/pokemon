package com.pokemon.inventory;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Map;
import java.util.stream.Collectors;

public class MainApp extends Application {

    private TableView<Card> table = new TableView<>();
    private TextField tfId = new TextField();
    private TextField tfName = new TextField();
    private ComboBox<String> cbType = new ComboBox<>();
    private ComboBox<String> cbRarity = new ComboBox<>();
    private TextField tfSetName = new TextField();
    private TextField tfQuantity = new TextField();
    private TextField tfImageUrl = new TextField();
    private ImageView imageView = new ImageView();
    private TextField tfSearch = new TextField();
    private ObservableList<Card> masterData = DataHandler.loadData(DataHandler.getDefaultFilePath());
    private FilteredList<Card> filteredData = new FilteredList<>(masterData, p -> true);
    private PokemonTcgApiService apiService = new PokemonTcgApiService();

    public static void main(String[] args) {
        System.setProperty("java.net.useSystemProxies", "true");
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Pokémon Card Inventory Manager");

        // 1. 设置表格列 (Table Columns)12
        TableColumn<Card, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(cellData -> cellData.getValue().idProperty());

        TableColumn<Card, String> colName = new TableColumn<>("Name");
        colName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());

        TableColumn<Card, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(cellData -> cellData.getValue().typeProperty());

        TableColumn<Card, String> colRarity = new TableColumn<>("Rarity");
        colRarity.setCellValueFactory(cellData -> cellData.getValue().rarityProperty());

        TableColumn<Card, String> colSet = new TableColumn<>("Set Name");
        colSet.setCellValueFactory(cellData -> cellData.getValue().setNameProperty());

        TableColumn<Card, Number> colQty = new TableColumn<>("Quantity");
        colQty.setCellValueFactory(cellData -> cellData.getValue().quantityProperty());

        table.getColumns().addAll(colId, colName, colType, colRarity, colSet, colQty);

        // 绑定过滤后的数据到表格
        SortedList<Card> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedData);

        // 表格点击事件：填充表单
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                populateForm(newSelection);
            }
        });

        //创建顶部菜单 (Menu Bar & Search)
        HBox topBar = createTopBar(primaryStage);

        // 创建底部表单 (Form & CRUD Buttons)
        VBox bottomForm = createBottomForm();

        //布局组装
        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(table);
        root.setBottom(bottomForm);

        Scene scene = new Scene(root, 900, 600);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();

        // 退出时自动保存
        primaryStage.setOnCloseRequest(event -> DataHandler.saveData(masterData, DataHandler.getDefaultFilePath()));
    }

    // --- GUI 组件构建方法 ---

    private HBox createTopBar(Stage stage) {
        // 搜索框
        Label searchLbl = new Label("Search / Filter: ");
        tfSearch.setPromptText("Type name, type, or set...");
        // 实时过滤逻辑
        tfSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(card -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                // 多字段匹配
                if (card.getName().toLowerCase().contains(lowerCaseFilter)) return true;
                if (card.getType().toLowerCase().contains(lowerCaseFilter)) return true;
                if (card.getSetName().toLowerCase().contains(lowerCaseFilter)) return true;
                return false;
            });
        });

        // 功能按钮
        Button btnStats = new Button("View Statistics");
        btnStats.setOnAction(e -> showStatistics());

        Button btnImport = new Button("Import CSV");
        btnImport.setOnAction(e -> handleImport(stage));

        Button btnExport = new Button("Export CSV");
        btnExport.setOnAction(e -> handleExport(stage));

        // Sync Data Button
        Button btnSyncData = new Button("Sync Data");
        btnSyncData.getStyleClass().add("sync-button");
        btnSyncData.setOnAction(e -> handleSyncData());

        // Local Search Button (replacing online search)
        Button btnSearchLocal = new Button("Search Local");
        btnSearchLocal.getStyleClass().add("local-search-button");
        btnSearchLocal.setOnAction(e -> showLocalSearchDialog());

        // Import Local DB Button
        Button btnImportDb = new Button("Import DB");
        btnImportDb.getStyleClass().add("import-db-button");
        btnImportDb.setOnAction(e -> handleImportDb());

        HBox topBox = new HBox(10, searchLbl, tfSearch, new Separator(), btnStats, btnImport, btnExport, btnSyncData, btnSearchLocal, btnImportDb);
        topBox.setPadding(new Insets(10));
        return topBox;
    }

    private void handleSyncData() {
        Alert loadingAlert = new Alert(Alert.AlertType.INFORMATION);
        loadingAlert.setTitle("Syncing Data");
        loadingAlert.setHeaderText("Downloading cards...");
        loadingAlert.setContentText("Fetching data from API. This may take a moment...");
        loadingAlert.show();

        new Thread(() -> {
            try {
                java.util.List<Card> allCards = new java.util.ArrayList<>();
                // Fetch only 1 page (250 cards) for testing connectivity
                // Reducing workload to prevent timeout on slow connections
                for (int i = 1; i <= 1; i++) {
                    java.util.List<Card> pageCards = apiService.fetchCardsByPage(i, 250);
                    allCards.addAll(pageCards);
                }
                
                DataHandler.saveLocalCache(allCards, DataHandler.getCacheFilePath());

                javafx.application.Platform.runLater(() -> {
                    loadingAlert.close();
                    showAlert("Success", "Downloaded " + allCards.size() + " cards to local cache.");
                });
            } catch (java.net.http.HttpTimeoutException e) {
                javafx.application.Platform.runLater(() -> {
                    loadingAlert.close();
                    askToLoadSampleData("Connection Timeout", "Server is too slow or down (Error 504).\nLoad sample data to continue development?");
                });
            } catch (java.io.IOException e) {
                javafx.application.Platform.runLater(() -> {
                    loadingAlert.close();
                    askToLoadSampleData("Network/Server Error", "Cannot connect to API (likely Error 504).\nLoad sample data to continue development?");
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    loadingAlert.close();
                    askToLoadSampleData("Unknown Error", "Sync failed: " + e.getMessage() + "\nLoad sample data?");
                });
            }
        }).start();
    }

    private void handleImportDb() {
        Alert loadingAlert = new Alert(Alert.AlertType.INFORMATION);
        loadingAlert.setTitle("Importing Database");
        loadingAlert.setHeaderText("Processing Local JSON Files...");
        loadingAlert.setContentText("This may take a minute depending on the number of files.");
        loadingAlert.show();

        new Thread(() -> {
            String result = DatabaseImporter.importData();
            javafx.application.Platform.runLater(() -> {
                loadingAlert.close();
                showAlert("Import Result", result);
            });
        }).start();
    }

    private void askToLoadSampleData(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText("API Connection Failed");
        alert.setContentText(content);
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                loadSampleData();
            }
        });
    }

    private void showLocalSearchDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Search Local Database");
        dialog.setHeaderText("Search in downloaded data");
        dialog.setContentText("Enter card name:");

        dialog.showAndWait().ifPresent(query -> {
            if (query.trim().isEmpty()) return;
            
            String lowerQuery = query.toLowerCase();
            ObservableList<Card> cachedCards = DataHandler.loadLocalCache(DataHandler.getCacheFilePath());
            
            if (cachedCards.isEmpty()) {
                showAlert("Info", "Local cache is empty. Please 'Sync Data' first.");
                return;
            }

            java.util.List<Card> results = cachedCards.stream()
                    .filter(c -> c.getName().toLowerCase().contains(lowerQuery))
                    .collect(Collectors.toList());

            showSearchResults(results);
        });
    }
    private VBox createBottomForm() {
        // 初始化下拉框
        cbType.getItems().addAll("Fire", "Water", "Grass", "Electric", "Psychic", "Fighting", "Darkness", "Metal", "Fairy", "Dragon", "Colorless");
        cbRarity.getItems().addAll("Common", "Uncommon", "Rare", "Ultra Rare", "Secret Rare");

        // Image Preview Setup
        imageView.setFitHeight(150);
        imageView.setFitWidth(100);
        imageView.setPreserveRatio(true);
        // imageView.setStyle("-fx-border-color: gray; -fx-border-width: 1;"); // Removed inline style
        Label lblImgPreview = new Label("No Image");
        StackPane imgPane = new StackPane(lblImgPreview, imageView);
        imgPane.setPrefSize(100, 150);
        imgPane.getStyleClass().add("image-view-wrapper");

        // Update Image when URL changes
        tfImageUrl.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) {
                try {
                    imageView.setImage(new Image(newVal, true)); // background loading
                    lblImgPreview.setVisible(false);
                } catch (Exception e) {
                    imageView.setImage(null);
                    lblImgPreview.setVisible(true);
                }
            } else {
                imageView.setImage(null);
                lblImgPreview.setVisible(true);
            }
        });

        // 表单布局
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        grid.add(new Label("Card ID:"), 0, 0);
        grid.add(tfId, 1, 0);
        grid.add(new Label("Name:"), 0, 1);
        grid.add(tfName, 1, 1);
        grid.add(new Label("Type:"), 2, 0);
        grid.add(cbType, 3, 0);
        grid.add(new Label("Rarity:"), 2, 1);
        grid.add(cbRarity, 3, 1);
        grid.add(new Label("Set Name:"), 4, 0);
        grid.add(tfSetName, 5, 0);
        grid.add(new Label("Quantity:"), 4, 1);
        grid.add(tfQuantity, 5, 1);
        grid.add(new Label("Image URL:"), 0, 2);
        grid.add(tfImageUrl, 1, 2, 5, 1);

        HBox formAndImage = new HBox(20, grid, imgPane);
        formAndImage.setPadding(new Insets(10));

        // CRUD 按钮
        Button btnAdd = new Button("Add Card");
        btnAdd.getStyleClass().add("primary-button");
        btnAdd.setOnAction(e -> addCard());

        Button btnUpdate = new Button("Update Selected");
        btnUpdate.getStyleClass().add("action-button");
        btnUpdate.setOnAction(e -> updateCard());

        Button btnDelete = new Button("Delete Selected");
        btnDelete.getStyleClass().add("danger-button");
        btnDelete.setOnAction(e -> deleteCard());

        Button btnClear = new Button("Clear Form");
        btnClear.setOnAction(e -> clearForm());

        HBox buttonBox = new HBox(10, btnAdd, btnUpdate, btnDelete, btnClear);
        buttonBox.setPadding(new Insets(10));

        VBox bottomBox = new VBox(formAndImage, buttonBox);
        bottomBox.setStyle("-fx-border-color: lightgray; -fx-border-width: 1px 0 0 0;");
        return bottomBox;
    }

    // --- 业务逻辑方法 (Controller Logic) ---

    private void showSearchResults(java.util.List<Card> results) {
        Stage resultStage = new Stage();
        resultStage.setTitle("Search Results");

        TableView<Card> resultTable = new TableView<>();
        
        TableColumn<Card, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(cellData -> cellData.getValue().idProperty());
        
        TableColumn<Card, String> colName = new TableColumn<>("Name");
        colName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        
        TableColumn<Card, String> colSet = new TableColumn<>("Set");
        colSet.setCellValueFactory(cellData -> cellData.getValue().setNameProperty());
        
        TableColumn<Card, String> colRarity = new TableColumn<>("Rarity");
        colRarity.setCellValueFactory(cellData -> cellData.getValue().rarityProperty());
        
        resultTable.getColumns().addAll(colName, colSet, colRarity, colId);
        resultTable.setItems(FXCollections.observableArrayList(results));

        Button btnImport = new Button("Import Selected");
        btnImport.setOnAction(e -> {
            Card selected = resultTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                // Check if already exists (optional, simply add for now)
                masterData.add(selected);
                DataHandler.saveData(masterData, DataHandler.getDefaultFilePath());
                showAlert("Success", "Card imported: " + selected.getName());
                resultStage.close();
            }
        });

        VBox layout = new VBox(10, resultTable, btnImport);
        layout.setPadding(new Insets(10));
        
        Scene scene = new Scene(layout, 600, 400);
        resultStage.setScene(scene);
        resultStage.show();
    }


    // --- 业务逻辑方法 (Controller Logic) ---

    private void addCard() {
        try {
            String id = tfId.getText();
            String name = tfName.getText();
            if (id.isEmpty() || name.isEmpty()) {
                showAlert("Error", "ID and Name are required!");
                return;
            }
            int qty = Integer.parseInt(tfQuantity.getText());

            Card newCard = new Card(
                    id, name,
                    cbType.getValue() != null ? cbType.getValue() : "N/A",
                    cbRarity.getValue() != null ? cbRarity.getValue() : "N/A",
                    tfSetName.getText(), qty,
                    tfImageUrl.getText()
            );

            masterData.add(newCard);
            clearForm();
            DataHandler.saveData(masterData, DataHandler.getDefaultFilePath()); // 自动保存
        } catch (NumberFormatException e) {
            showAlert("Error", "Quantity must be a number.");
        }
    }

    private void updateCard() {
        Card selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "No card selected.");
            return;
        }
        try {
            selected.setId(tfId.getText());
            selected.setName(tfName.getText());
            selected.setType(cbType.getValue());
            selected.setRarity(cbRarity.getValue());
            selected.setSetName(tfSetName.getText());
            selected.setQuantity(Integer.parseInt(tfQuantity.getText()));
            selected.setImageUrl(tfImageUrl.getText());

            table.refresh(); // 刷新视图
            clearForm();
            DataHandler.saveData(masterData, DataHandler.getDefaultFilePath());
        } catch (NumberFormatException e) {
            showAlert("Error", "Quantity must be a number.");
        }
    }

    private void deleteCard() {
        Card selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            masterData.remove(selected);
            clearForm();
            DataHandler.saveData(masterData, DataHandler.getDefaultFilePath());
        } else {
            showAlert("Warning", "No card selected.");
        }
    }

    private void populateForm(Card card) {
        tfId.setText(card.getId());
        tfName.setText(card.getName());
        cbType.setValue(card.getType());
        cbRarity.setValue(card.getRarity());
        tfSetName.setText(card.getSetName());
        tfQuantity.setText(String.valueOf(card.getQuantity()));
        tfImageUrl.setText(card.getImageUrl());
    }

    private void clearForm() {
        tfId.clear();
        tfName.clear();
        cbType.setValue(null);
        cbRarity.setValue(null);
        tfSetName.clear();
        tfQuantity.clear();
        tfImageUrl.clear();
        table.getSelectionModel().clearSelection();
    }


    private void showStatistics() {
        Map<String, Integer> stats = masterData.stream()
                .collect(Collectors.groupingBy(Card::getType, Collectors.summingInt(Card::getQuantity)));

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        stats.forEach((type, count) -> pieChartData.add(new PieChart.Data(type, count)));

        PieChart chart = new PieChart(pieChartData);
        chart.setTitle("Inventory by Type");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Statistics");
        alert.setHeaderText("Total Cards: " + masterData.stream().mapToInt(Card::getQuantity).sum());
        alert.getDialogPane().setContent(chart);
        alert.showAndWait();
    }

    private void handleImport(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Inventory CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            ObservableList<Card> imported = DataHandler.loadData(file.getAbsolutePath());
            masterData.addAll(imported);
            showAlert("Success", "Imported " + imported.size() + " cards.");
        }
    }

    private void handleExport(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Inventory CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            DataHandler.saveData(masterData, file.getAbsolutePath());
            showAlert("Success", "Data exported successfully.");
        }
    }

    private void loadSampleData() {
        masterData.add(new Card("base1-1", "Alakazam", "Psychic", "Rare Holo", "Base", 1, "https://images.pokemontcg.io/base1/1.png"));
        masterData.add(new Card("base1-4", "Charizard", "Fire", "Rare Holo", "Base", 1, "https://images.pokemontcg.io/base1/4.png"));
        masterData.add(new Card("base1-15", "Venusaur", "Grass", "Rare Holo", "Base", 1, "https://images.pokemontcg.io/base1/15.png"));
        masterData.add(new Card("base1-24", "Charmeleon", "Fire", "Uncommon", "Base", 2, "https://images.pokemontcg.io/base1/24.png"));
        masterData.add(new Card("base1-42", "Wartortle", "Water", "Uncommon", "Base", 2, "https://images.pokemontcg.io/base1/42.png"));
        masterData.add(new Card("base1-58", "Pikachu", "Lightning", "Common", "Base", 4, "https://images.pokemontcg.io/base1/58.png"));
        
        DataHandler.saveData(masterData, DataHandler.getDefaultFilePath());
        showAlert("Success", "Loaded " + 6 + " sample cards.");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
