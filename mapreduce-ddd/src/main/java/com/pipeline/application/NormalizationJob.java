package com.pipeline.application;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import io.github.cdimascio.dotenv.Dotenv;

import com.pipeline.infrastructure.mapreduce.FoodFactsMapper;
import com.pipeline.infrastructure.mapreduce.FoodFactsReducer;

/**
 * Single entrypoint for the OpenFoodFacts normalization pipeline.
 *
 * Hadoop splits the input into 128MB InputSplits — each mapper processes
 * only its chunk via S3 range requests. A 1TB file creates ~8000 parallel mappers.
 *
 * Usage:
 *   hadoop jar mapreduce-ddd-1.0.0.jar \
 *     com.pipeline.application.NormalizationJob \
 *     s3a://bucket/foodfacts.csv s3a://bucket/output/
 */
public class NormalizationJob extends Configured implements Tool {

    @Override
    public int run(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: NormalizationJob <input-path> <output-path>");
            System.err.println("  input-path:  s3a://bucket/foodfacts.csv");
            System.err.println("  output-path: s3a://bucket/output/");
            return 1;
        }

        Configuration conf = getConf();
        loadEnvConfig(conf);

        Job job = Job.getInstance(conf, "openfoodfacts-normalization");
        job.setJarByClass(NormalizationJob.class);

        job.setMapperClass(FoodFactsMapper.class);
        job.setReducerClass(FoodFactsReducer.class);

        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        job.setNumReduceTasks(conf.getInt("pipeline.num.reducers", 64));

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        return job.waitForCompletion(true) ? 0 : 1;
    }

    private void loadEnvConfig(Configuration conf) {
        Dotenv dotenv = Dotenv.configure()
                .directory("../")
                .filename(".env")
                .ignoreIfMissing()
                .load();

        // Database
        setIfPresent(conf, "pipeline.db.url", dotenv.get("POSTGRES_URL"));
        setIfPresent(conf, "pipeline.db.user", dotenv.get("POSTGRES_USER"));
        setIfPresent(conf, "pipeline.db.password", dotenv.get("POSTGRES_PASSWORD"));

        // S3
        String accessKey = dotenv.get("S3_ACCESS_KEY");
        String secretKey = dotenv.get("S3_SECRET_KEY");
        String endpoint = dotenv.get("S3_ENDPOINT");

        if (accessKey != null && conf.get("fs.s3a.access.key") == null) {
            conf.set("fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");
            conf.set("fs.s3a.access.key", accessKey);
            conf.set("fs.s3a.secret.key", secretKey);
            if (endpoint != null && !endpoint.contains("amazonaws.com")) {
                conf.set("fs.s3a.endpoint", endpoint);
                conf.set("fs.s3a.path.style.access", "true");
            }
        }
    }

    private void setIfPresent(Configuration conf, String key, String value) {
        if (value != null) {
            conf.set(key, value);
        }
    }

    public static void main(String[] args) throws Exception {
        int exitCode = ToolRunner.run(new Configuration(), new NormalizationJob(), args);
        System.exit(exitCode);
    }
}
