package com.pokemon.inventory;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class DataHandler {

    private static final String FILE_NAME = "inventory_data.csv";


    public static void saveData(ObservableList<Card> cardList, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {
            for (Card card : cardList) {
                writer.write(card.toCSV());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static ObservableList<Card> loadData(String filePath) {
        ObservableList<Card> list = FXCollections.observableArrayList();
        File file = new File(filePath);
        if (!file.exists()) return list;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Card card = Card.fromCSV(line);
                if (card != null) {
                    list.add(card);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static String getDefaultFilePath() {
        return FILE_NAME;
    }
}