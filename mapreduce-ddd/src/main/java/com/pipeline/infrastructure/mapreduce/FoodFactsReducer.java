package com.pipeline.infrastructure.mapreduce;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import com.pipeline.domain.model.Product;
import com.pipeline.domain.valueobject.Barcode;
import com.pipeline.domain.valueobject.NutriScore;
import com.pipeline.domain.valueobject.NutrientInfo;
import com.pipeline.infrastructure.config.DatabaseConfig;
import com.pipeline.infrastructure.persistence.BatchWriter;

public class FoodFactsReducer extends Reducer<Text, Text, Text, NullWritable> {

    private static final String PRODUCT_SQL = """
            INSERT INTO products (barcode, name, generic_name, quantity,
                nutriscore_score, nutriscore_grade, nova_group,
                energy_kcal, fat, saturated_fat, sugars, salt, proteins, fiber)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (barcode) DO UPDATE SET
                name = COALESCE(EXCLUDED.name, products.name),
                generic_name = COALESCE(EXCLUDED.generic_name, products.generic_name),
                quantity = COALESCE(EXCLUDED.quantity, products.quantity),
                nutriscore_score = EXCLUDED.nutriscore_score,
                nutriscore_grade = COALESCE(EXCLUDED.nutriscore_grade, products.nutriscore_grade),
                nova_group = EXCLUDED.nova_group,
                energy_kcal = EXCLUDED.energy_kcal, fat = EXCLUDED.fat,
                saturated_fat = EXCLUDED.saturated_fat, sugars = EXCLUDED.sugars,
                salt = EXCLUDED.salt, proteins = EXCLUDED.proteins, fiber = EXCLUDED.fiber
            """;

    private static final Map<String, String> RELATION_TABLES = Map.ofEntries(
            Map.entry("packaging", "product_packaging"),
            Map.entry("brands", "product_brands"),
            Map.entry("categories", "product_categories"),
            Map.entry("origins", "product_origins"),
            Map.entry("labels", "product_labels"),
            Map.entry("countries", "product_countries"),
            Map.entry("ingredients", "product_ingredients"),
            Map.entry("ingredients_analysis", "product_ingredients_analysis"),
            Map.entry("allergens", "product_allergens"),
            Map.entry("traces", "product_traces"),
            Map.entry("additives", "product_additives"),
            Map.entry("states", "product_states"),
            Map.entry("nutrient_levels", "product_nutrient_levels")
    );

    private DatabaseConfig config;
    private BatchWriter productBatch;
    private Map<String, BatchWriter> relationBatches;

    @Override
    protected void setup(Context context) {
        try {
            config = new DatabaseConfig(context.getConfiguration());
            Connection conn = config.getConnection();

            productBatch = new BatchWriter(conn, PRODUCT_SQL);

            relationBatches = new HashMap<>();
            for (Map.Entry<String, String> entry : RELATION_TABLES.entrySet()) {
                String sql = "INSERT INTO " + entry.getValue()
                        + " (product_barcode, value) VALUES (?, ?) ON CONFLICT DO NOTHING";
                relationBatches.put(entry.getKey(), new BatchWriter(conn, sql));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize reducer", e);
        }
    }

    @Override
    public void reduce(Text barcodeKey, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {

        String barcode = barcodeKey.toString();
        Product product = null;

        try {
            for (Text val : values) {
                String line = val.toString();

                if (line.startsWith("PRODUCT\t")) {
                    String[] p = line.substring(8).split("\t", -1);
                    if (p.length < 13) continue;

                    product = new Product(
                            new Barcode(barcode),
                            p[0], p[1], p[2],
                            new NutriScore(parseIntSafe(p[3]), p[4].isBlank() ? null : p[4]),
                            parseIntSafe(p[5]),
                            new NutrientInfo(
                                    parseDoubleSafe(p[6]), parseDoubleSafe(p[7]),
                                    parseDoubleSafe(p[8]), parseDoubleSafe(p[9]),
                                    parseDoubleSafe(p[10]), parseDoubleSafe(p[11]),
                                    parseDoubleSafe(p[12])
                            )
                    );
                } else if (line.startsWith("REL\t")) {
                    String[] parts = line.substring(4).split("\t", 2);
                    if (parts.length == 2) {
                        if (product == null) {
                            product = new Product(
                                    new Barcode(barcode), null, null, null,
                                    new NutriScore(0, null), 0,
                                    new NutrientInfo(0, 0, 0, 0, 0, 0, 0)
                            );
                        }
                        product.addRelation(parts[0], parts[1]);
                    }
                }
            }

            if (product == null) return;

            // Batch insert product
            var ps = productBatch.getStatement();
            ps.setString(1, product.getBarcode().getValue());
            ps.setString(2, product.getName());
            ps.setString(3, product.getGenericName());
            ps.setString(4, product.getQuantity());
            ps.setInt(5, product.getNutriScore().getScore());
            ps.setString(6, product.getNutriScore().getGrade());
            ps.setInt(7, product.getNovaGroup());
            ps.setDouble(8, product.getNutrientInfo().getEnergyKcal());
            ps.setDouble(9, product.getNutrientInfo().getFat());
            ps.setDouble(10, product.getNutrientInfo().getSaturatedFat());
            ps.setDouble(11, product.getNutrientInfo().getSugars());
            ps.setDouble(12, product.getNutrientInfo().getSalt());
            ps.setDouble(13, product.getNutrientInfo().getProteins());
            ps.setDouble(14, product.getNutrientInfo().getFiber());
            productBatch.addToBatch();

            // Batch insert relations
            for (var entry : product.getRelations().entrySet()) {
                BatchWriter batch = relationBatches.get(entry.getKey());
                if (batch == null) continue;
                var relStmt = batch.getStatement();
                for (String value : entry.getValue()) {
                    relStmt.setString(1, barcode);
                    relStmt.setString(2, value);
                    batch.addToBatch();
                }
            }

            context.write(new Text(barcode), NullWritable.get());

        } catch (SQLException e) {
            throw new IOException("DB error for barcode " + barcode, e);
        }
    }

    @Override
    protected void cleanup(Context context) {
        try {
            productBatch.close();
            for (BatchWriter batch : relationBatches.values()) {
                batch.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to flush batches", e);
        } finally {
            config.close();
        }
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
