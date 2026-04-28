import os

from dotenv import load_dotenv
from pyspark.sql import SparkSession
from pyspark.sql.functions import explode, split, col, trim, lower

load_dotenv(dotenv_path="../.env")

POSTGRES_URL = os.getenv("POSTGRES_URL")
POSTGRES_USER = os.getenv("POSTGRES_USER")
POSTGRES_PASSWORD = os.getenv("POSTGRES_PASSWORD")

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

# 2. Products table — scalar columns only
products = df.select(
    col("code").alias("barcode"),
    col("product_name").alias("name"),
    col("generic_name"),
    col("quantity"),
    col("nutriscore_score").cast("int"),
    col("nutriscore_grade"),
    col("nova_group").cast("int"),
    col("energy_kcal100g").cast("double").alias("energy_kcal"),
    col("fat100g").cast("double").alias("fat"),
    col("saturated_fat100g").cast("double").alias("saturated_fat"),
    col("sugars100g").cast("double").alias("sugars"),
    col("salt100g").cast("double").alias("salt"),
    col("proteins100g").cast("double").alias("proteins"),
    col("fiber100g").cast("double").alias("fiber"),
)

products.write.mode("append").jdbc(POSTGRES_URL, "products", properties=jdbc_properties)

# 3. Normalize each comma-separated column into its own dedicated table
RELATION_COLUMNS = {
    "packaging_tags":              "product_packaging",
    "brands":                      "product_brands",
    "categories_tags":             "product_categories",
    "origins_tags":                "product_origins",
    "labels_tags":                 "product_labels",
    "countries_tags":              "product_countries",
    "ingredients_tags":            "product_ingredients",
    "ingredients_analysis_tags":   "product_ingredients_analysis",
    "allergens":                   "product_allergens",
    "traces_tags":                 "product_traces",
    "additives_tags":              "product_additives",
    "states_tags":                 "product_states",
    "nutrient_levels_tags":        "product_nutrient_levels",
}

for csv_column, table_name in RELATION_COLUMNS.items():
    if csv_column not in df.columns:
        continue

    relations = df.select(
        col("code").alias("product_barcode"),
        explode(split(col(csv_column), ",")).alias("value"),
    )
    relations = relations \
        .withColumn("value", lower(trim(col("value")))) \
        .filter(col("value") != "") \
        .select("product_barcode", "value")

    relations.write.mode("append").jdbc(
        POSTGRES_URL, table_name, properties=jdbc_properties
    )

spark.stop()
