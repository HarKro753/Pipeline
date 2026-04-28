package com.pipeline.infrastructure.mapreduce;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;

public class FoodFactsMapper extends Mapper<LongWritable, Text, Text, Text> {

    @Override
    public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
        String line = value.toString();
        String[] columns = line.split("\t");

        if (columns.length < 25) return;

        String barcode = columns[0];
        String productName = columns[11];
        String brand = columns[19];
        String categoriesTags = columns[23];

        // Emit product data keyed by barcode
        String productData = String.join("|", barcode, productName, brand);
        context.write(new Text("PRODUCT"), new Text(productData));

        // Emit tag relations: one entry per tag
        if (categoriesTags != null && !categoriesTags.isEmpty()) {
            String[] tags = categoriesTags.split(",");
            for (String tag : tags) {
                String trimmed = tag.trim();
                if (!trimmed.isEmpty()) {
                    context.write(new Text("TAG:" + trimmed), new Text(barcode));
                }
            }
        }
    }
}
