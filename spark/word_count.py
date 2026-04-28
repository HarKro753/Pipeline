from pyspark.sql import SparkSession
from pyspark.sql.functions import explode, split, lower, col

spark = SparkSession.builder.appName("WordCount").getOrCreate()

# Read input text file
lines = spark.read.text("../sample_text.txt")

# Split lines into words, count occurrences
words = lines.select(explode(split(lower(col("value")), "\\s+")).alias("word"))
word_counts = words.groupBy("word").count().orderBy("count", ascending=False)

word_counts.show(20)
word_counts.write.mode("overwrite").csv("output/word_counts")

spark.stop()
