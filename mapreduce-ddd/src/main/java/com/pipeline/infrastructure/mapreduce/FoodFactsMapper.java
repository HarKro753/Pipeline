package com.pipeline.infrastructure.mapreduce;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import com.pipeline.domain.model.Product;
import com.pipeline.domain.parser.CsvProductParser;

public class FoodFactsMapper extends Mapper<LongWritable, Text, Text, Text> {

    private static final char DELIM = '\t';

    private final CsvProductParser parser = new CsvProductParser();

    @Override
    public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
        Optional<Product> parsed = parser.parse(value.toString());
        if (parsed.isEmpty()) return;

        Product product = parsed.get();
        Text barcodeKey = new Text(product.getBarcode().getValue());

        // Emit scalar product data with tab delimiter
        String productData = "PRODUCT" + DELIM
                + sanitize(product.getName()) + DELIM
                + sanitize(product.getGenericName()) + DELIM
                + sanitize(product.getQuantity()) + DELIM
                + product.getNutriScore().getScore() + DELIM
                + (product.getNutriScore().getGrade() != null ? product.getNutriScore().getGrade() : "") + DELIM
                + product.getNovaGroup() + DELIM
                + product.getNutrientInfo().getEnergyKcal() + DELIM
                + product.getNutrientInfo().getFat() + DELIM
                + product.getNutrientInfo().getSaturatedFat() + DELIM
                + product.getNutrientInfo().getSugars() + DELIM
                + product.getNutrientInfo().getSalt() + DELIM
                + product.getNutrientInfo().getProteins() + DELIM
                + product.getNutrientInfo().getFiber();
        context.write(barcodeKey, new Text(productData));

        // Emit each relation value
        for (Map.Entry<String, List<String>> entry : product.getRelations().entrySet()) {
            String category = entry.getKey();
            for (String val : entry.getValue()) {
                context.write(barcodeKey, new Text("REL" + DELIM + category + DELIM + sanitize(val)));
            }
        }
    }

    private static String sanitize(String value) {
        if (value == null) return "";
        return value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }
}
