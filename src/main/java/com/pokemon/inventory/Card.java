package com.pokemon.inventory;

import javafx.beans.property.*;

public class Card {
    private final StringProperty id;
    private final StringProperty name;
    private final StringProperty type;
    private final StringProperty rarity;
    private final StringProperty setName;
    private final IntegerProperty quantity;
    // 1. Add the new property
    private final StringProperty imageUrl;

    // 2. Update Constructor to accept 7 arguments
    public Card(String id, String name, String type, String rarity, String setName, int quantity, String imageUrl) {
        this.id = new SimpleStringProperty(id);
        this.name = new SimpleStringProperty(name);
        this.type = new SimpleStringProperty(type);
        this.rarity = new SimpleStringProperty(rarity);
        this.setName = new SimpleStringProperty(setName);
        this.quantity = new SimpleIntegerProperty(quantity);
        // Initialize the new property
        this.imageUrl = new SimpleStringProperty(imageUrl);
    }

    public StringProperty idProperty() { return id; }
    public StringProperty nameProperty() { return name; }
    public StringProperty typeProperty() { return type; }
    public StringProperty rarityProperty() { return rarity; }
    public StringProperty setNameProperty() { return setName; }
    public IntegerProperty quantityProperty() { return quantity; }
    // 3. Add Property Accessor
    public StringProperty imageUrlProperty() { return imageUrl; }

    // Standard Getters
    public String getId() { return id.get(); }
    public String getName() { return name.get(); }
    public String getType() { return type.get(); }
    public String getRarity() { return rarity.get(); }
    public String getSetName() { return setName.get(); }
    public int getQuantity() { return quantity.get(); }
    // 4. Add Getter
    public String getImageUrl() { return imageUrl.get(); }

    // Setters
    public void setId(String id) { this.id.set(id); }
    public void setName(String name) { this.name.set(name); }
    public void setType(String type) { this.type.set(type); }
    public void setRarity(String rarity) { this.rarity.set(rarity); }
    public void setSetName(String setName) { this.setName.set(setName); }
    public void setQuantity(int quantity) { this.quantity.set(quantity); }
    // 5. Add Setter
    public void setImageUrl(String imageUrl) { this.imageUrl.set(imageUrl); }

    public String toCSV() {
        // 6. Update toCSV to include the image URL
        return id.get() + "," + name.get() + "," + type.get() + "," +
                rarity.get() + "," + setName.get() + "," + quantity.get() + "," + imageUrl.get();
    }

    public static Card fromCSV(String csvLine) {
        String[] parts = csvLine.split(",");
        // 7. Update logic to handle 7 parts
        // Added a check for >= 6 to maintain backward compatibility with old files
        if (parts.length >= 6) {
            String imgUrl = (parts.length > 6) ? parts[6] : ""; // Default to empty string if missing
            return new Card(parts[0], parts[1], parts[2], parts[3], parts[4], Integer.parseInt(parts[5]), imgUrl);
        }
        return null;
    }
}