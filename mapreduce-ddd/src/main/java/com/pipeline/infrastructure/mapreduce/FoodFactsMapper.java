package com.pipeline.infrastructure.mapreduce;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import com.pipeline.domain.parser.CsvProductParser;
import com.pipeline.domain.model.Product;

/**
 * Thin mapper: parses CSV via CsvProductParser, then emits all data keyed by barcode.
 * Keying by barcode ensures even distribution across reducers at TB scale.
 *
 * Emits:
 *   (barcode, "PRODUCT|name|genericName|quantity|nutriScore|grade|nova|kcal|fat|satFat|sugars|salt|proteins|fiber")
 *   (barcode, "REL|category|value")
 */
public class FoodFactsMapper extends Mapper<LongWritable, Text, Text, Text> {

    private final CsvProductParser parser = new CsvProductParser();

    @Override
    public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
        Optional<Product> parsed = parser.parse(value.toString());
        if (parsed.isEmpty()) return;

        Product product = parsed.get();
        Text barcodeKey = new Text(product.getBarcode().getValue());

        // Emit scalar product data
        String productData = "PRODUCT|" + String.join("|",
                product.getName(),
                product.getGenericName(),
                product.getQuantity(),
                String.valueOf(product.getNutriScore().getScore()),
                product.getNutriScore().getGrade() != null ? product.getNutriScore().getGrade() : "",
                String.valueOf(product.getNovaGroup()),
                String.valueOf(product.getNutrientInfo().getEnergyKcal()),
                String.valueOf(product.getNutrientInfo().getFat()),
                String.valueOf(product.getNutrientInfo().getSaturatedFat()),
                String.valueOf(product.getNutrientInfo().getSugars()),
                String.valueOf(product.getNutrientInfo().getSalt()),
                String.valueOf(product.getNutrientInfo().getProteins()),
                String.valueOf(product.getNutrientInfo().getFiber())
        );
        context.write(barcodeKey, new Text(productData));

        // Emit each relation value
        for (Map.Entry<String, List<String>> entry : product.getRelations().entrySet()) {
            String category = entry.getKey();
            for (String val : entry.getValue()) {
                context.write(barcodeKey, new Text("REL|" + category + "|" + val));
            }
        }
    }
}
