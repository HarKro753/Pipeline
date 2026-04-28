package com.pipeline.infrastructure.mapreduce;

import java.io.IOException;
import java.util.Map;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import com.pipeline.domain.repository.ProductAdditiveRepository;
import com.pipeline.domain.repository.ProductAllergenRepository;
import com.pipeline.domain.repository.ProductBrandRepository;
import com.pipeline.domain.repository.ProductCategoryRepository;
import com.pipeline.domain.repository.ProductCountryRepository;
import com.pipeline.domain.repository.ProductIngredientAnalysisRepository;
import com.pipeline.domain.repository.ProductIngredientRepository;
import com.pipeline.domain.repository.ProductLabelRepository;
import com.pipeline.domain.repository.ProductNutrientLevelRepository;
import com.pipeline.domain.repository.ProductOriginRepository;
import com.pipeline.domain.repository.ProductPackagingRepository;
import com.pipeline.domain.repository.ProductStateRepository;
import com.pipeline.domain.repository.ProductTraceRepository;
import com.pipeline.infrastructure.config.DatabaseConfig;
import com.pipeline.infrastructure.persistence.PostgresProductAdditiveRepository;
import com.pipeline.infrastructure.persistence.PostgresProductAllergenRepository;
import com.pipeline.infrastructure.persistence.PostgresProductBrandRepository;
import com.pipeline.infrastructure.persistence.PostgresProductCategoryRepository;
import com.pipeline.infrastructure.persistence.PostgresProductCountryRepository;
import com.pipeline.infrastructure.persistence.PostgresProductIngredientAnalysisRepository;
import com.pipeline.infrastructure.persistence.PostgresProductIngredientRepository;
import com.pipeline.infrastructure.persistence.PostgresProductLabelRepository;
import com.pipeline.infrastructure.persistence.PostgresProductNutrientLevelRepository;
import com.pipeline.infrastructure.persistence.PostgresProductOriginRepository;
import com.pipeline.infrastructure.persistence.PostgresProductPackagingRepository;
import com.pipeline.infrastructure.persistence.PostgresProductRepository;
import com.pipeline.infrastructure.persistence.PostgresProductStateRepository;
import com.pipeline.infrastructure.persistence.PostgresProductTraceRepository;

public class FoodFactsReducer extends Reducer<Text, Text, Text, NullWritable> {

    private PostgresProductRepository productRepository;
    private Map<String, Object> relationRepositories;

    @Override
    protected void setup(Context context) {
        DatabaseConfig config = new DatabaseConfig();
        productRepository = new PostgresProductRepository(config);

        relationRepositories = Map.ofEntries(
                Map.entry("product_packaging", new PostgresProductPackagingRepository(config)),
                Map.entry("product_brands", new PostgresProductBrandRepository(config)),
                Map.entry("product_categories", new PostgresProductCategoryRepository(config)),
                Map.entry("product_origins", new PostgresProductOriginRepository(config)),
                Map.entry("product_labels", new PostgresProductLabelRepository(config)),
                Map.entry("product_countries", new PostgresProductCountryRepository(config)),
                Map.entry("product_ingredients", new PostgresProductIngredientRepository(config)),
                Map.entry("product_ingredients_analysis", new PostgresProductIngredientAnalysisRepository(config)),
                Map.entry("product_allergens", new PostgresProductAllergenRepository(config)),
                Map.entry("product_traces", new PostgresProductTraceRepository(config)),
                Map.entry("product_additives", new PostgresProductAdditiveRepository(config)),
                Map.entry("product_states", new PostgresProductStateRepository(config)),
                Map.entry("product_nutrient_levels", new PostgresProductNutrientLevelRepository(config))
        );
    }

    @Override
    public void reduce(Text key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {
        String tableName = key.toString();

        if (tableName.equals("PRODUCT")) {
            for (Text val : values) {
                String[] p = val.toString().split("\\|", -1);
                if (p.length < 14) continue;

                productRepository.saveRaw(
                        p[0], p[1], p[2], p[3],
                        parseIntSafe(p[4]), p[5], parseIntSafe(p[6]),
                        parseDoubleSafe(p[7]), parseDoubleSafe(p[8]),
                        parseDoubleSafe(p[9]), parseDoubleSafe(p[10]),
                        parseDoubleSafe(p[11]), parseDoubleSafe(p[12]),
                        parseDoubleSafe(p[13])
                );
                context.write(val, NullWritable.get());
            }
        } else {
            Object repo = relationRepositories.get(tableName);
            if (repo == null) return;

            for (Text val : values) {
                String[] parts = val.toString().split("\\|", 2);
                if (parts.length < 2) continue;
                invokeSave(repo, parts[0], parts[1]);
            }
            context.write(new Text(tableName + " done"), NullWritable.get());
        }
    }

    private void invokeSave(Object repo, String barcode, String value) {
        if (repo instanceof ProductPackagingRepository r) r.save(barcode, value);
        else if (repo instanceof ProductBrandRepository r) r.save(barcode, value);
        else if (repo instanceof ProductCategoryRepository r) r.save(barcode, value);
        else if (repo instanceof ProductOriginRepository r) r.save(barcode, value);
        else if (repo instanceof ProductLabelRepository r) r.save(barcode, value);
        else if (repo instanceof ProductCountryRepository r) r.save(barcode, value);
        else if (repo instanceof ProductIngredientRepository r) r.save(barcode, value);
        else if (repo instanceof ProductIngredientAnalysisRepository r) r.save(barcode, value);
        else if (repo instanceof ProductAllergenRepository r) r.save(barcode, value);
        else if (repo instanceof ProductTraceRepository r) r.save(barcode, value);
        else if (repo instanceof ProductAdditiveRepository r) r.save(barcode, value);
        else if (repo instanceof ProductStateRepository r) r.save(barcode, value);
        else if (repo instanceof ProductNutrientLevelRepository r) r.save(barcode, value);
    }

    private static int parseIntSafe(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    private static double parseDoubleSafe(String s) {
        try { return Double.parseDouble(s.trim()); }
        catch (Exception e) { return 0.0; }
    }
}
