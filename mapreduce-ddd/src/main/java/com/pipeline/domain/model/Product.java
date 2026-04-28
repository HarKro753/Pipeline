package com.pipeline.domain.model;

import com.pipeline.domain.valueobject.Barcode;
import com.pipeline.domain.valueobject.NutriScore;
import com.pipeline.domain.valueobject.NutrientInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Product {

    private final Barcode barcode;
    private final String name;
    private final String brand;
    private final String genericName;
    private final String quantity;
    private final NutriScore nutriScore;
    private final int novaGroup;
    private final NutrientInfo nutrientInfo;
    private final List<ProductTag> tags;

    public Product(Barcode barcode, String name, String brand, String genericName,
                   String quantity, NutriScore nutriScore, int novaGroup,
                   NutrientInfo nutrientInfo, List<ProductTag> tags) {
        if (barcode == null) {
            throw new IllegalArgumentException("Product must have a barcode");
        }
        this.barcode = barcode;
        this.name = name;
        this.brand = brand;
        this.genericName = genericName;
        this.quantity = quantity;
        this.nutriScore = nutriScore;
        this.novaGroup = novaGroup;
        this.nutrientInfo = nutrientInfo;
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
    }

    public Barcode getBarcode() { return barcode; }
    public String getName() { return name; }
    public String getBrand() { return brand; }
    public String getGenericName() { return genericName; }
    public String getQuantity() { return quantity; }
    public NutriScore getNutriScore() { return nutriScore; }
    public int getNovaGroup() { return novaGroup; }
    public NutrientInfo getNutrientInfo() { return nutrientInfo; }
    public List<ProductTag> getTags() { return Collections.unmodifiableList(tags); }

    public void addTag(String category, String tagValue) {
        ProductTag tag = new ProductTag(barcode, category, tagValue);
        if (!tags.contains(tag)) {
            tags.add(tag);
        }
    }

    @Override
    public String toString() {
        return "Product{barcode=" + barcode + ", name='" + name + "', brand='" + brand + "'}";
    }
}
