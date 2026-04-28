package com.pipeline.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pipeline.domain.valueobject.Barcode;
import com.pipeline.domain.valueobject.NutriScore;
import com.pipeline.domain.valueobject.NutrientInfo;

public class Product {

    private final Barcode barcode;
    private final String name;
    private final String genericName;
    private final String quantity;
    private final NutriScore nutriScore;
    private final int novaGroup;
    private final NutrientInfo nutrientInfo;
    private final Map<String, List<String>> relations;

    public Product(Barcode barcode, String name, String genericName,
                   String quantity, NutriScore nutriScore, int novaGroup,
                   NutrientInfo nutrientInfo) {
        if (barcode == null) {
            throw new IllegalArgumentException("Product must have a barcode");
        }
        this.barcode = barcode;
        this.name = name;
        this.genericName = genericName;
        this.quantity = quantity;
        this.nutriScore = nutriScore;
        this.novaGroup = novaGroup;
        this.nutrientInfo = nutrientInfo;
        this.relations = new HashMap<>();
    }

    public void addRelation(String category, String value) {
        if (category == null || value == null || value.isBlank()) return;
        relations.computeIfAbsent(category, k -> new ArrayList<>()).add(value.trim().toLowerCase());
    }

    public Barcode getBarcode() { return barcode; }
    public String getName() { return name; }
    public String getGenericName() { return genericName; }
    public String getQuantity() { return quantity; }
    public NutriScore getNutriScore() { return nutriScore; }
    public int getNovaGroup() { return novaGroup; }
    public NutrientInfo getNutrientInfo() { return nutrientInfo; }
    public Map<String, List<String>> getRelations() { return Collections.unmodifiableMap(relations); }
    public List<String> getRelation(String category) {
        return Collections.unmodifiableList(relations.getOrDefault(category, List.of()));
    }

    @Override
    public String toString() {
        return "Product{barcode=" + barcode + ", name='" + name + "'}";
    }
}
