package com.pipeline.infrastructure.mapreduce;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;
import java.util.Map;

public class FoodFactsMapper extends Mapper<LongWritable, Text, Text, Text> {

    /**
     * Each entry maps a CSV column index to its dedicated relation table name.
     * The mapper emits (TABLE_NAME, barcode|value) for each comma-separated value.
     */
    private static final Map<Integer, String> RELATION_COLUMNS = Map.ofEntries(
            Map.entry(15, "product_packaging"),
            Map.entry(19, "product_brands"),
            Map.entry(23, "product_categories"),
            Map.entry(25, "product_origins"),
            Map.entry(30, "product_labels"),
            Map.entry(40, "product_countries"),
            Map.entry(43, "product_ingredients"),
            Map.entry(44, "product_ingredients_analysis"),
            Map.entry(46, "product_allergens"),
            Map.entry(48, "product_traces"),
            Map.entry(55, "product_additives"),
            Map.entry(66, "product_states"),
            Map.entry(72, "product_nutrient_levels")
    );

    @Override
    public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
        if (key.get() == 0 && value.toString().startsWith("code")) return;

        String[] columns = value.toString().split("\t", -1);
        if (columns.length < 160) return;

        String barcode = columns[0];
        if (barcode == null || barcode.isBlank()) return;

        // Emit scalar product data
        String productData = String.join("|",
                barcode,
                columns[11],  // product_name
                columns[12],  // generic_name
                columns[13],  // quantity
                columns[57],  // nutriscore_score
                columns[58],  // nutriscore_grade
                columns[59],  // nova_group
                columns[90],  // energy_kcal100g
                columns[94],  // fat100g
                columns[95],  // saturated_fat100g
                columns[140], // sugars100g
                columns[157], // salt100g
                columns[155], // proteins100g
                columns[152]  // fiber100g
        );
        context.write(new Text("PRODUCT"), new Text(productData));

        // Emit each comma-separated column to its own table
        for (var entry : RELATION_COLUMNS.entrySet()) {
            int colIndex = entry.getKey();
            String tableName = entry.getValue();

            if (colIndex >= columns.length) continue;
            String cellValue = columns[colIndex];
            if (cellValue == null || cellValue.isBlank()) continue;

            for (String tag : cellValue.split(",")) {
                String trimmed = tag.trim();
                if (!trimmed.isEmpty()) {
                    context.write(new Text(tableName), new Text(barcode + "|" + trimmed));
                }
            }
        }
    }
}
