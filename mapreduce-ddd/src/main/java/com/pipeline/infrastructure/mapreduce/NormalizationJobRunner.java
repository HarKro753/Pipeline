package com.pipeline.infrastructure.mapreduce;

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

/**
 * Entrypoint for the OpenFoodFacts normalization MapReduce job.
 *
 * How splitting works at TB scale:
 * - Hadoop's TextInputFormat splits the input file into InputSplits (default 128MB each)
 * - For a 1TB CSV, this creates ~8000 splits → 8000 mappers running in parallel
 * - Each mapper only downloads and processes its own 128MB chunk, NOT the entire file
 * - S3A filesystem handles range requests so each mapper reads only its byte range
 * - TextInputFormat respects line boundaries across splits automatically
 *
 * Usage:
 *   hadoop jar mapreduce-ddd-1.0.0.jar \
 *     com.pipeline.infrastructure.mapreduce.NormalizationJobRunner \
 *     s3a://pipeline-data/foodfacts.csv s3a://pipeline-data/output/
 */
public class NormalizationJobRunner extends Configured implements Tool {

    @Override
    public int run(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: NormalizationJobRunner <input-path> <output-path>");
            System.err.println("  input-path:  s3a://bucket/path/to/foodfacts.csv");
            System.err.println("  output-path: s3a://bucket/path/to/output/");
            return 1;
        }

        Configuration conf = getConf();

        // Load S3 credentials from .env if not already set via Hadoop config
        configureS3(conf);

        // Pass DB credentials to mappers/reducers via Hadoop configuration
        configureDatabase(conf);

        Job job = Job.getInstance(conf, "openfoodfacts-normalization");
        job.setJarByClass(NormalizationJobRunner.class);

        job.setMapperClass(FoodFactsMapper.class);
        job.setReducerClass(FoodFactsReducer.class);

        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        // Parallelism: 64 reducers by default, tune based on cluster + DB capacity
        job.setNumReduceTasks(conf.getInt("pipeline.num.reducers", 64));

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        return job.waitForCompletion(true) ? 0 : 1;
    }

    private void configureS3(Configuration conf) {
        Dotenv dotenv = Dotenv.configure().directory("../").filename(".env").ignoreIfMissing().load();

        String endpoint = dotenv.get("S3_ENDPOINT");
        String accessKey = dotenv.get("S3_ACCESS_KEY");
        String secretKey = dotenv.get("S3_SECRET_KEY");

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

    private void configureDatabase(Configuration conf) {
        Dotenv dotenv = Dotenv.configure().directory("../").filename(".env").ignoreIfMissing().load();

        String dbUrl = dotenv.get("POSTGRES_URL");
        String dbUser = dotenv.get("POSTGRES_USER");
        String dbPassword = dotenv.get("POSTGRES_PASSWORD");

        if (dbUrl != null) {
            conf.set("pipeline.db.url", dbUrl);
            conf.set("pipeline.db.user", dbUser);
            conf.set("pipeline.db.password", dbPassword);
        }
    }

    public static void main(String[] args) throws Exception {
        int exitCode = ToolRunner.run(new Configuration(), new NormalizationJobRunner(), args);
        System.exit(exitCode);
    }
}
