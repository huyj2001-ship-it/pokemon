package com.pokemon.inventory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class DataHandler {

    private static final String FILE_NAME = "inventory_data.csv";
    private static final String CACHE_FILE_NAME = "local_card_database.json";
    private static final Gson gson = new Gson();

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

    public static void saveLocalCache(List<Card> cardList, String filePath) {
        JsonArray jsonArray = new JsonArray();
        for (Card card : cardList) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", card.getId());
            obj.addProperty("name", card.getName());
            obj.addProperty("type", card.getType());
            obj.addProperty("rarity", card.getRarity());
            obj.addProperty("setName", card.getSetName());
            obj.addProperty("quantity", card.getQuantity());
            obj.addProperty("imageUrl", card.getImageUrl());
            jsonArray.add(obj);
        }

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {
            gson.toJson(jsonArray, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ObservableList<Card> loadLocalCache(String filePath) {
        ObservableList<Card> list = FXCollections.observableArrayList();
        File file = new File(filePath);
        if (!file.exists()) return list;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);
            if (jsonArray != null) {
                for (JsonElement element : jsonArray) {
                    JsonObject obj = element.getAsJsonObject();
                    String id = obj.has("id") ? obj.get("id").getAsString() : "";
                    String name = obj.has("name") ? obj.get("name").getAsString() : "";
                    String type = obj.has("type") ? obj.get("type").getAsString() : "N/A";
                    String rarity = obj.has("rarity") ? obj.get("rarity").getAsString() : "Common";
                    String setName = obj.has("setName") ? obj.get("setName").getAsString() : "";
                    int quantity = obj.has("quantity") ? obj.get("quantity").getAsInt() : 1;
                    String imageUrl = obj.has("imageUrl") ? obj.get("imageUrl").getAsString() : "";

                    list.add(new Card(id, name, type, rarity, setName, quantity, imageUrl));
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
    
    public static String getCacheFilePath() {
        return CACHE_FILE_NAME;
    }
}