# OpenFoodFacts Normalization Pipeline

A Hadoop MapReduce pipeline that normalizes the OpenFoodFacts dataset (1TB+ CSV, 200+ columns) into a relational PostgreSQL schema. Built with **Domain-Driven Design** in Java.

## Highlights

- Normalizes 13 comma-separated CSV columns into dedicated relation tables
- Java MapReduce with **DDD architecture** (Domain, Application, Infrastructure layers)
- Batch DB writes (1000 rows/flush) for TB-scale throughput
- S3-compatible input, PostgreSQL output
- K8s manifests + CI/CD for staging and production
- [AWS cost analysis](docs/aws-cost-analysis.md) for daily 1TB processing

## Architecture

```
mapreduce-ddd/
├── domain/                  # Pure business logic, no framework deps
│   ├── model/               # Product (aggregate root)
│   ├── valueobject/         # Barcode, NutriScore, NutrientInfo
│   ├── parser/              # CsvProductParser
│   └── repository/          # Interfaces only
├── application/             # Single entrypoint
│   └── NormalizationJob     # Configures and launches the Hadoop job
└── infrastructure/          # Framework & DB implementations
    ├── config/              # DatabaseConfig (Hadoop conf based)
    ├── persistence/         # Postgres repositories + BatchWriter
    └── mapreduce/           # Mapper, Reducer
```

### DDD Layers

| Layer              | Responsibility                         | Dependencies |
| ------------------ | -------------------------------------- | ------------ |
| **Domain**         | Business rules, entities, value objects | None         |
| **Application**    | Job entrypoint and orchestration       | Domain       |
| **Infrastructure** | Hadoop, PostgreSQL, config             | Domain       |

### How It Scales to Terabytes

The input CSV lives on S3. Hadoop's `TextInputFormat` splits it into **128MB InputSplits** — each mapper only downloads its own chunk via S3 range requests. A 1TB file creates ~8000 mappers running in parallel. No single node touches the full file.

```
S3: foodfacts.csv (1 TB)
  ├── Split 0 (128MB) → Mapper 0 → (barcode₁, PRODUCT\t...)
  ├── Split 1 (128MB) → Mapper 1 → (barcode₂, REL\tallergens\tmilk)
  └── Split N (128MB) → Mapper N → ...
                              ↓ Shuffle & Sort (group by barcode)
  ├── Reducer 0 → barcode₁: full Product aggregate → BatchWriter → Postgres
  ├── Reducer 1 → barcode₂: full Product aggregate → BatchWriter → Postgres
  └── ...64 reducers
```

1. **Map** — `CsvProductParser` parses each row into a `Product` domain object. Mapper emits `(barcode, data)` keyed by barcode for even distribution
2. **Shuffle & Sort** — Hadoop groups all values for the same barcode onto one reducer
3. **Reduce** — Rebuilds the full `Product` aggregate, batch-inserts into Postgres (1000 rows/flush)

## Usage

```bash
cd mapreduce-ddd

# Build
mvn clean package

# Run normalization from S3 (writes to Postgres)
hadoop jar target/mapreduce-ddd-1.0.0.jar \
  com.pipeline.application.NormalizationJob \
  -Dpipeline.num.reducers=64 \
  s3a://pipeline-data/foodfacts.csv s3a://pipeline-data/output/
```

## Environment Setup

1. Copy the example env file and fill in your credentials:

```bash
cp .env.example .env
```

2. Configure your `.env`:

```env
POSTGRES_URL=jdbc:postgresql://localhost:5432/pipeline_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_password
S3_ENDPOINT=https://s3.amazonaws.com
S3_ACCESS_KEY=your_access_key
S3_SECRET_KEY=your_secret_key
```

3. Create the database tables:

```sql
CREATE TABLE products (
    barcode VARCHAR(14) PRIMARY KEY,
    name TEXT,
    generic_name TEXT,
    quantity TEXT,
    nutriscore_score INT,
    nutriscore_grade VARCHAR(1),
    nova_group INT,
    energy_kcal DOUBLE PRECISION,
    fat DOUBLE PRECISION,
    saturated_fat DOUBLE PRECISION,
    sugars DOUBLE PRECISION,
    salt DOUBLE PRECISION,
    proteins DOUBLE PRECISION,
    fiber DOUBLE PRECISION
);

CREATE TABLE product_packaging        (id SERIAL PRIMARY KEY, product_barcode VARCHAR(14) REFERENCES products(barcode), value TEXT NOT NULL, UNIQUE(product_barcode, value));
CREATE TABLE product_brands           (id SERIAL PRIMARY KEY, product_barcode VARCHAR(14) REFERENCES products(barcode), value TEXT NOT NULL, UNIQUE(product_barcode, value));
CREATE TABLE product_categories       (id SERIAL PRIMARY KEY, product_barcode VARCHAR(14) REFERENCES products(barcode), value TEXT NOT NULL, UNIQUE(product_barcode, value));
CREATE TABLE product_origins          (id SERIAL PRIMARY KEY, product_barcode VARCHAR(14) REFERENCES products(barcode), value TEXT NOT NULL, UNIQUE(product_barcode, value));
CREATE TABLE product_labels           (id SERIAL PRIMARY KEY, product_barcode VARCHAR(14) REFERENCES products(barcode), value TEXT NOT NULL, UNIQUE(product_barcode, value));
CREATE TABLE product_countries        (id SERIAL PRIMARY KEY, product_barcode VARCHAR(14) REFERENCES products(barcode), value TEXT NOT NULL, UNIQUE(product_barcode, value));
CREATE TABLE product_ingredients      (id SERIAL PRIMARY KEY, product_barcode VARCHAR(14) REFERENCES products(barcode), value TEXT NOT NULL, UNIQUE(product_barcode, value));
CREATE TABLE product_ingredients_analysis (id SERIAL PRIMARY KEY, product_barcode VARCHAR(14) REFERENCES products(barcode), value TEXT NOT NULL, UNIQUE(product_barcode, value));
CREATE TABLE product_allergens        (id SERIAL PRIMARY KEY, product_barcode VARCHAR(14) REFERENCES products(barcode), value TEXT NOT NULL, UNIQUE(product_barcode, value));
CREATE TABLE product_traces           (id SERIAL PRIMARY KEY, product_barcode VARCHAR(14) REFERENCES products(barcode), value TEXT NOT NULL, UNIQUE(product_barcode, value));
CREATE TABLE product_additives        (id SERIAL PRIMARY KEY, product_barcode VARCHAR(14) REFERENCES products(barcode), value TEXT NOT NULL, UNIQUE(product_barcode, value));
CREATE TABLE product_states           (id SERIAL PRIMARY KEY, product_barcode VARCHAR(14) REFERENCES products(barcode), value TEXT NOT NULL, UNIQUE(product_barcode, value));
CREATE TABLE product_nutrient_levels  (id SERIAL PRIMARY KEY, product_barcode VARCHAR(14) REFERENCES products(barcode), value TEXT NOT NULL, UNIQUE(product_barcode, value));
```

## Prerequisites

- **Java 17+** and **Maven 3.8+**
- **Hadoop 3.3+** or **AWS EMR**
- **PostgreSQL 15+** or **Amazon RDS**

## License

This project is licensed under the [MIT License](LICENSE).

## Contributing

Found an issue or want to improve the pipeline? Open an issue or submit a pull request.
