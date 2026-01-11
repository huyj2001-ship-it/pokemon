package com.pokemon.inventory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class PokemonTcgApiService {

    private static final String API_KEY = "16fe6b17-55be-4eab-95c8-3bab505bcb4c";
    private static final String API_URL = "https://api.pokemontcg.io/v2/cards";

    private final HttpClient httpClient;
    private final Gson gson;

    public PokemonTcgApiService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();
        this.gson = new Gson();
    }

    public List<Card> searchCards(String query) throws IOException, InterruptedException {
        // Optimize 1: Remove leading wildcard to allow index usage on DB side (faster)
        // Optimize 2: Limit page size to 30 to reduce payload
        // Optimize 3: Select only necessary fields to reduce payload
        String encodedQuery = URLEncoder.encode("name:" + query + "*", StandardCharsets.UTF_8);
        String url = API_URL + "?q=" + encodedQuery + "&pageSize=30&select=id,name,types,rarity,set.name,images.small";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-Api-Key", API_KEY)
                .header("Accept", "application/json")
                .timeout(java.time.Duration.ofSeconds(10)) // Request timeout
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("API request failed with status code: " + response.statusCode());
        }

        return parseResponse(response.body());
    }

    public List<Card> fetchCardsByPage(int page, int pageSize) throws IOException, InterruptedException {
        String url = API_URL + "?page=" + page + "&pageSize=" + pageSize + "&select=id,name,types,rarity,set.name,images.small";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-Api-Key", API_KEY)
                .header("Accept", "application/json")
                .timeout(java.time.Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("API request failed: " + response.statusCode());
        }
        return parseResponse(response.body());
    }

    private List<Card> parseResponse(String jsonBody) {
        List<Card> cardList = new ArrayList<>();
        JsonObject jsonObject = gson.fromJson(jsonBody, JsonObject.class);

        if (jsonObject.has("data")) {
            JsonArray dataArray = jsonObject.getAsJsonArray("data");
            for (JsonElement element : dataArray) {
                JsonObject obj = element.getAsJsonObject();

                String id = getAsString(obj, "id");
                String name = getAsString(obj, "name");
                
                String type = "N/A";
                if (obj.has("types")) {
                    JsonArray types = obj.getAsJsonArray("types");
                    if (types.size() > 0) {
                        type = types.get(0).getAsString();
                    }
                }

                String rarity = getAsString(obj, "rarity");
                if (rarity.isEmpty()) rarity = "Common"; // Default fallback

                String setName = "";
                if (obj.has("set")) {
                    setName = getAsString(obj.getAsJsonObject("set"), "name");
                }

                String imageUrl = "";
                if (obj.has("images")) {
                    JsonObject images = obj.getAsJsonObject("images");
                    if (images.has("small")) {
                        imageUrl = images.get("small").getAsString();
                    }
                }

                // Default quantity to 1 for new imports
                cardList.add(new Card(id, name, type, rarity, setName, 1, imageUrl));
            }
        }
        return cardList;
    }

    private String getAsString(JsonObject obj, String memberName) {
        if (obj.has(memberName) && !obj.get(memberName).isJsonNull()) {
            return obj.get(memberName).getAsString();
        }
        return "";
    }
}
