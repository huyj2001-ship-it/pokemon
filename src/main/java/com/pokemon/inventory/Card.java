package com.pokemon.inventory;

import javafx.beans.property.*;

public class Card {
    private final StringProperty id;
    private final StringProperty name;
    private final StringProperty type;
    private final StringProperty rarity;
    private final StringProperty setName;
    private final IntegerProperty quantity;


    public Card(String id, String name, String type, String rarity, String setName, int quantity) {
        this.id = new SimpleStringProperty(id);
        this.name = new SimpleStringProperty(name);
        this.type = new SimpleStringProperty(type);
        this.rarity = new SimpleStringProperty(rarity);
        this.setName = new SimpleStringProperty(setName);
        this.quantity = new SimpleIntegerProperty(quantity);
    }


    public StringProperty idProperty() { return id; }
    public StringProperty nameProperty() { return name; }
    public StringProperty typeProperty() { return type; }
    public StringProperty rarityProperty() { return rarity; }
    public StringProperty setNameProperty() { return setName; }
    public IntegerProperty quantityProperty() { return quantity; }

    // Standard Getters
    public String getId() { return id.get(); }
    public String getName() { return name.get(); }
    public String getType() { return type.get(); }
    public String getRarity() { return rarity.get(); }
    public String getSetName() { return setName.get(); }
    public int getQuantity() { return quantity.get(); }

    // Setters
    public void setId(String id) { this.id.set(id); }
    public void setName(String name) { this.name.set(name); }
    public void setType(String type) { this.type.set(type); }
    public void setRarity(String rarity) { this.rarity.set(rarity); }
    public void setSetName(String setName) { this.setName.set(setName); }
    public void setQuantity(int quantity) { this.quantity.set(quantity); }


    public String toCSV() {
        return id.get() + "," + name.get() + "," + type.get() + "," +
                rarity.get() + "," + setName.get() + "," + quantity.get();
    }


    public static Card fromCSV(String csvLine) {
        String[] parts = csvLine.split(",");
        if (parts.length == 6) {
            return new Card(parts[0], parts[1], parts[2], parts[3], parts[4], Integer.parseInt(parts[5]));
        }
        return null;
    }
}