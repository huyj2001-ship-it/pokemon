package com.pokemon.inventory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseImporter {

    private static final String DATA_DIR = "pokemon-tcg-data-2.15";
    private static final String SETS_FILE = DATA_DIR + "/sets/en.json";
    private static final String CARDS_DIR = DATA_DIR + "/cards/en";

    public static String importData() {
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            return "Error: Data directory '" + DATA_DIR + "' not found. Please ensure the pokemon-tcg-data folder is in the project root.";
        }

        Gson gson = new Gson();
        Map<String, String> setNames = new HashMap<>();
        List<Card> allCards = new ArrayList<>();

        // 1. Load Sets
        try (FileReader reader = new FileReader(SETS_FILE, StandardCharsets.UTF_8)) {
            JsonArray setsArray = gson.fromJson(reader, JsonArray.class);
            for (JsonElement el : setsArray) {
                JsonObject setObj = el.getAsJsonObject();
                String id = setObj.get("id").getAsString();
                String name = setObj.get("name").getAsString();
                setNames.put(id, name);
            }
        } catch (IOException e) {
            return "Error loading sets: " + e.getMessage();
        }

        // 2. Load Cards
        File cardsDir = new File(CARDS_DIR);
        File[] cardFiles = cardsDir.listFiles((dir, name) -> name.endsWith(".json"));

        if (cardFiles == null || cardFiles.length == 0) {
            return "Error: No card JSON files found in " + CARDS_DIR;
        }

        int filesProcessed = 0;
        for (File cardFile : cardFiles) {
            String filename = cardFile.getName();
            String setId = filename.replace(".json", "");
            String setName = setNames.getOrDefault(setId, setId);

            try (FileReader reader = new FileReader(cardFile, StandardCharsets.UTF_8)) {
                JsonArray cardsArray = gson.fromJson(reader, JsonArray.class); // Assuming top level is array
                if (cardsArray != null) {
                     for (JsonElement el : cardsArray) {
                        JsonObject cardObj = el.getAsJsonObject();
                        
                        String id = cardObj.has("id") ? cardObj.get("id").getAsString() : "unknown";
                        String name = cardObj.has("name") ? cardObj.get("name").getAsString() : "Unknown";
                        
                        String type = "N/A";
                        if (cardObj.has("types")) {
                            JsonArray types = cardObj.getAsJsonArray("types");
                            if (types.size() > 0) {
                                type = types.get(0).getAsString();
                            }
                        }
                        
                        String rarity = cardObj.has("rarity") ? cardObj.get("rarity").getAsString() : "Common";
                        
                        String imageUrl = "";
                        if (cardObj.has("images")) {
                            JsonObject images = cardObj.getAsJsonObject("images");
                            if (images.has("small")) {
                                imageUrl = images.get("small").getAsString();
                            }
                        }

                        // Create Card (quantity 0 for reference)
                        Card card = new Card(id, name, type, rarity, setName, 0, imageUrl);
                        allCards.add(card);
                     }
                }
                filesProcessed++;
            } catch (Exception e) {
                System.err.println("Failed to process " + filename + ": " + e.getMessage());
            }
        }

        // 3. Save to local cache
        DataHandler.saveLocalCache(allCards, DataHandler.getCacheFilePath());

        return "Success: Imported " + allCards.size() + " cards from " + filesProcessed + " sets.";
    }
}
