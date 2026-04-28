import os

from dotenv import load_dotenv
from pyspark.sql import SparkSession
from pyspark.sql.functions import explode, split, col, trim, lower

load_dotenv(dotenv_path="../.env")

POSTGRES_URL = os.getenv("POSTGRES_URL", "").replace("jdbc:", "")
POSTGRES_USER = os.getenv("POSTGRES_USER")
POSTGRES_PASSWORD = os.getenv("POSTGRES_PASSWORD")

jdbc_url = os.getenv("POSTGRES_URL")
jdbc_properties = {
    "user": POSTGRES_USER,
    "password": POSTGRES_PASSWORD,
    "driver": "org.postgresql.Driver",
}

spark = SparkSession.builder \
    .appName("FoodFactsNormalization") \
    .config("spark.jars", "/path/to/postgresql-42.7.3.jar") \
    .getOrCreate()

# 1. Read the tab-separated OpenFoodFacts CSV
df = spark.read.option("sep", "\t").csv("../sample.csv", header=True)

# 2. Products table — simple projection
products = df.select(
    col("code").alias("barcode"),
    col("product_name").alias("name"),
    col("brands").alias("brand"),
    col("nutriscore_score"),
    col("nutriscore_grade"),
    col("energy_kcal100g").alias("energy_kcal"),
    col("fat100g").alias("fat"),
    col("saturated_fat100g").alias("saturated_fat"),
    col("sugars100g").alias("sugars"),
    col("salt100g").alias("salt"),
    col("proteins100g").alias("proteins"),
    col("fiber100g").alias("fiber"),
)

products.write.mode("append").jdbc(jdbc_url, "products", properties=jdbc_properties)

# 3. Normalize tags — explode the comma-separated categories into rows
tag_relations = df.select(
    col("code").alias("product_barcode"),
    explode(split(col("categories_tags"), ",")).alias("tag_name"),
)
tag_relations = tag_relations.withColumn("tag_name", lower(trim(col("tag_name"))))

tag_relations.write.mode("append").jdbc(jdbc_url, "product_tags_flat", properties=jdbc_properties)

spark.stop()
