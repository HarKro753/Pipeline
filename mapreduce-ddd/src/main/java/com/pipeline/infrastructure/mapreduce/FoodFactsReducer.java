package com.pipeline.infrastructure.mapreduce;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import com.pipeline.domain.model.Product;
import com.pipeline.domain.repository.ProductRelationRepository;
import com.pipeline.domain.repository.ProductRepository;
import com.pipeline.domain.valueobject.Barcode;
import com.pipeline.domain.valueobject.NutriScore;
import com.pipeline.domain.valueobject.NutrientInfo;
import com.pipeline.infrastructure.config.DatabaseConfig;
import com.pipeline.infrastructure.persistence.*;

public class FoodFactsReducer extends Reducer<Text, Text, Text, NullWritable> {

    private ProductRepository productRepository;
    private Map<String, ProductRelationRepository> relationRepositories;
    private DatabaseConfig config;

    @Override
    protected void setup(Context context) {
        config = new DatabaseConfig(context.getConfiguration());
        productRepository = new PostgresProductRepository(config);

        relationRepositories = Map.ofEntries(
                Map.entry("packaging", new PostgresProductPackagingRepository(config)),
                Map.entry("brands", new PostgresProductBrandRepository(config)),
                Map.entry("categories", new PostgresProductCategoryRepository(config)),
                Map.entry("origins", new PostgresProductOriginRepository(config)),
                Map.entry("labels", new PostgresProductLabelRepository(config)),
                Map.entry("countries", new PostgresProductCountryRepository(config)),
                Map.entry("ingredients", new PostgresProductIngredientRepository(config)),
                Map.entry("ingredients_analysis", new PostgresProductIngredientAnalysisRepository(config)),
                Map.entry("allergens", new PostgresProductAllergenRepository(config)),
                Map.entry("traces", new PostgresProductTraceRepository(config)),
                Map.entry("additives", new PostgresProductAdditiveRepository(config)),
                Map.entry("states", new PostgresProductStateRepository(config)),
                Map.entry("nutrient_levels", new PostgresProductNutrientLevelRepository(config))
        );
    }

    @Override
    public void reduce(Text barcodeKey, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {

        String barcode = barcodeKey.toString();
        Product product = null;

        for (Text val : values) {
            String line = val.toString();

            if (line.startsWith("PRODUCT|")) {
                String[] p = line.substring(8).split("\\|", -1);
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
            } else if (line.startsWith("REL|")) {
                if (product == null) {
                    product = new Product(
                            new Barcode(barcode), null, null, null,
                            new NutriScore(0, null), 0,
                            new NutrientInfo(0, 0, 0, 0, 0, 0, 0)
                    );
                }
                String[] parts = line.substring(4).split("\\|", 2);
                if (parts.length == 2) {
                    product.addRelation(parts[0], parts[1]);
                }
            }
        }

        if (product == null) return;

        // Persist product
        productRepository.save(product);

        // Persist all relations
        for (Map.Entry<String, List<String>> entry : product.getRelations().entrySet()) {
            ProductRelationRepository repo = relationRepositories.get(entry.getKey());
            if (repo == null) continue;
            for (String value : entry.getValue()) {
                repo.save(barcode, value);
            }
        }

        context.write(new Text(barcode), NullWritable.get());
    }

    @Override
    protected void cleanup(Context context) {
        config.close();
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
