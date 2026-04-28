package com.pipeline.infrastructure.mapreduce;

import java.io.IOException;
import java.util.Map;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import com.pipeline.application.parser.CsvProductParser;
import com.pipeline.application.service.NormalizationService;
import com.pipeline.domain.model.Product;
import com.pipeline.domain.repository.ProductRelationRepository;
import com.pipeline.domain.valueobject.Barcode;
import com.pipeline.domain.valueobject.NutriScore;
import com.pipeline.domain.valueobject.NutrientInfo;
import com.pipeline.infrastructure.config.DatabaseConfig;
import com.pipeline.infrastructure.persistence.*;

public class FoodFactsReducer extends Reducer<Text, Text, Text, NullWritable> {

    private NormalizationService normalizationService;

    @Override
    protected void setup(Context context) {
        DatabaseConfig config = new DatabaseConfig();

        Map<String, ProductRelationRepository> repos = Map.ofEntries(
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

        normalizationService = new NormalizationService(
                new PostgresProductRepository(config),
                repos
        );
    }

    @Override
    public void reduce(Text key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {
        String keyStr = key.toString();

        if (keyStr.equals("PRODUCT")) {
            for (Text val : values) {
                String[] p = val.toString().split("\\|", -1);
                if (p.length < 14) continue;

                Product product = new Product(
                        new Barcode(p[0]),
                        p[1], p[2], p[3],
                        new NutriScore(parseIntSafe(p[4]), p[5].isBlank() ? null : p[5]),
                        parseIntSafe(p[6]),
                        new NutrientInfo(
                                parseDoubleSafe(p[7]), parseDoubleSafe(p[8]),
                                parseDoubleSafe(p[9]), parseDoubleSafe(p[10]),
                                parseDoubleSafe(p[11]), parseDoubleSafe(p[12]),
                                parseDoubleSafe(p[13])
                        )
                );
                // Product without relations — just persist scalar data
                normalizationService.normalize(product);
                context.write(val, NullWritable.get());
            }
        } else {
            // Relation category — rebuild a minimal product per barcode and normalize
            for (Text val : values) {
                String[] parts = val.toString().split("\\|", 2);
                if (parts.length < 2) continue;

                Product product = new Product(
                        new Barcode(parts[0]), null, null, null,
                        new NutriScore(0, null), 0,
                        new NutrientInfo(0, 0, 0, 0, 0, 0, 0)
                );
                product.addRelation(keyStr, parts[1]);
                normalizationService.normalize(product);
            }
            context.write(new Text(keyStr + " done"), NullWritable.get());
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
