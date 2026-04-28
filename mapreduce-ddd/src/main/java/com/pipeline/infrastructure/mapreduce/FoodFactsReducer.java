package com.pipeline.infrastructure.mapreduce;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.StringJoiner;

public class FoodFactsReducer extends Reducer<Text, Text, Text, NullWritable> {

    @Override
    public void reduce(Text key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {
        String keyStr = key.toString();

        if (keyStr.equals("PRODUCT")) {
            // Pass through product records
            for (Text val : values) {
                context.write(val, NullWritable.get());
            }
        } else if (keyStr.startsWith("TAG:")) {
            // Aggregate all product codes for this tag
            String tagName = keyStr.substring(4);
            StringJoiner joiner = new StringJoiner(",");
            for (Text val : values) {
                joiner.add(val.toString());
            }
            String output = tagName + "|" + joiner;
            context.write(new Text(output), NullWritable.get());
        }
    }
}
