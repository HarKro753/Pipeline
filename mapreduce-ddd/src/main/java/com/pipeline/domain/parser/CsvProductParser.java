package com.pipeline.domain.parser;

import java.util.Map;
import java.util.Optional;

import com.pipeline.domain.model.Product;
import com.pipeline.domain.valueobject.Barcode;
import com.pipeline.domain.valueobject.NutriScore;
import com.pipeline.domain.valueobject.NutrientInfo;

public class CsvProductParser {

    private static final Map<Integer, String> RELATION_COLUMNS = Map.ofEntries(
            Map.entry(15, "packaging"),
            Map.entry(19, "brands"),
            Map.entry(23, "categories"),
            Map.entry(25, "origins"),
            Map.entry(30, "labels"),
            Map.entry(40, "countries"),
            Map.entry(43, "ingredients"),
            Map.entry(44, "ingredients_analysis"),
            Map.entry(46, "allergens"),
            Map.entry(48, "traces"),
            Map.entry(55, "additives"),
            Map.entry(66, "states"),
            Map.entry(72, "nutrient_levels")
    );

    public Optional<Product> parse(String tsvLine) {
        if (tsvLine == null || tsvLine.startsWith("code")) {
            return Optional.empty();
        }

        String[] col = tsvLine.split("\t", -1);
        if (col.length < 160) {
            return Optional.empty();
        }

        String barcodeRaw = col[0];
        if (barcodeRaw == null || barcodeRaw.isBlank()) {
            return Optional.empty();
        }

        Barcode barcode;
        try {
            barcode = new Barcode(barcodeRaw);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }

        NutriScore nutriScore = new NutriScore(
                parseIntSafe(col[57]),
                col[58].isBlank() ? null : col[58]
        );

        NutrientInfo nutrients = new NutrientInfo(
                parseDoubleSafe(col[90]),   // energy_kcal
                parseDoubleSafe(col[94]),   // fat
                parseDoubleSafe(col[95]),   // saturated_fat
                parseDoubleSafe(col[140]),  // sugars
                parseDoubleSafe(col[157]),  // salt
                parseDoubleSafe(col[155]),  // proteins
                parseDoubleSafe(col[152])   // fiber
        );

        Product product = new Product(
                barcode,
                col[11],  // product_name
                col[12],  // generic_name
                col[13],  // quantity
                nutriScore,
                parseIntSafe(col[59]),  // nova_group
                nutrients
        );

        // Parse all comma-separated relation columns
        for (var entry : RELATION_COLUMNS.entrySet()) {
            int colIndex = entry.getKey();
            String category = entry.getValue();

            if (colIndex >= col.length) continue;
            String cellValue = col[colIndex];
            if (cellValue == null || cellValue.isBlank()) continue;

            for (String value : cellValue.split(",")) {
                product.addRelation(category, value);
            }
        }

        return Optional.of(product);
    }

    private static int parseIntSafe(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (Exception e) { return 0; }
    }

    private static double parseDoubleSafe(String s) {
        try { return Double.parseDouble(s.trim()); }
        catch (Exception e) { return 0.0; }
    }
}
