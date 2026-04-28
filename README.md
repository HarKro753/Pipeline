# Batch Processing Pipeline — MapReduce vs Apache Spark

A side-by-side comparison of batch processing approaches using real-world **OpenFoodFacts** data: classic **Hadoop MapReduce** (Java, DDD architecture) vs **Apache Spark** (Python).

## Highlights

- Hands-on comparison of MapReduce and Spark with identical use cases
- Java MapReduce project structured with **Domain-Driven Design** (Domain, Application, Infrastructure layers)
- Real-world normalization of OpenFoodFacts CSV (200+ columns, N:M tag relations)
- PostgreSQL persistence with connection via `.env`
- Detailed [comparison document](docs/comparison.md) with code snippets and trade-offs

## Architecture

```
mapreduce-ddd/
├── domain/                  # Pure business logic, no framework deps
│   ├── model/               # Product (aggregate root), Tag, ProductTag
│   ├── valueobject/         # Barcode, NutriScore, NutrientInfo
│   └── repository/          # Interfaces only
├── application/             # Use case orchestration, DTOs
│   ├── service/             # NormalizationService
│   └── dto/                 # ProductDTO, TagRelationDTO
└── infrastructure/          # Framework & DB implementations
    ├── config/              # DatabaseConfig (.env loader)
    ├── persistence/         # Postgres repository implementations
    └── mapreduce/           # Hadoop Mapper/Reducer classes
```

### DDD Layers

| Layer              | Responsibility                          | Dependencies                    |
| ------------------ | --------------------------------------- | ------------------------------- |
| **Domain**         | Business rules, entities, value objects | None                            |
| **Application**    | Orchestrates use cases                  | Domain                          |
| **Infrastructure** | Hadoop, PostgreSQL, config              | Domain, Application, Frameworks |

### How It Scales to Terabytes

The input CSV lives on S3-compatible storage. Hadoop's `TextInputFormat` splits it into **128MB InputSplits** — each mapper only downloads and processes its own chunk via S3 range requests. A 1TB file creates ~8000 mappers running in parallel across the cluster. No single node ever touches the full file.

```
S3: foodfacts.csv (1 TB)
  ├── Split 0 (128MB) → Mapper 0 → (barcode₁, PRODUCT|...), (barcode₁, REL|allergens|milk)
  ├── Split 1 (128MB) → Mapper 1 → (barcode₂, PRODUCT|...), (barcode₂, REL|countries|en:france)
  ├── ...
  └── Split N (128MB) → Mapper N → ...
                                ↓ Shuffle & Sort (group by barcode)
  ├── Reducer 0 → barcode₁: full Product aggregate → Postgres
  ├── Reducer 1 → barcode₂: full Product aggregate → Postgres
  └── ...
```

1. **Map** — `CsvProductParser` parses each row into a `Product` domain object. Mapper emits `(barcode, data)` pairs keyed by barcode for even distribution
2. **Shuffle & Sort** — Hadoop groups all values for the same barcode onto one reducer
3. **Reduce** — Rebuilds the full `Product` aggregate, calls `NormalizationService.normalize()` to persist to Postgres

## Comparison at a Glance

|                          | MapReduce                 | Spark                             |
| ------------------------ | ------------------------- | --------------------------------- |
| **Language**             | Java                      | Python                            |
| **Intermediate storage** | Disk (HDFS)               | In-memory (RAM)                   |
| **Execution model**      | Map → Reduce              | DAG (flexible pipeline)           |
| **Speed**                | Baseline                  | 10–100x faster                    |
| **Best for**             | Simple ETL, huge datasets | Iterative, interactive, streaming |

See [docs/comparison.md](docs/comparison.md) for the full breakdown.

## Usage

### MapReduce (Java)

```bash
cd mapreduce-ddd

# Build
mvn clean package

# Run OpenFoodFacts normalization (writes to Postgres)
# Run OpenFoodFacts normalization from S3 (writes to Postgres)
hadoop jar target/mapreduce-ddd-1.0.0.jar \
  com.pipeline.infrastructure.mapreduce.NormalizationJobRunner \
  -Dpipeline.num.reducers=64 \
  s3a://pipeline-data/foodfacts.csv s3a://pipeline-data/output/
```

### Spark (Python)

```bash
cd spark
pip install -r requirements.txt

# OpenFoodFacts normalization (writes to Postgres)
spark-submit --jars /path/to/postgresql-42.7.3.jar normalize_foodfacts.py
```

## Environment Setup

1. Copy the example env file and fill in your Postgres credentials:

```bash
cp .env.example .env
```

2. Configure your `.env`:

```env
POSTGRES_URL=jdbc:postgresql://localhost:5432/pipeline_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_password
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

-- Each comma-separated CSV column gets its own relation table
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
- **Hadoop 3.3+** (for MapReduce execution)
- **Python 3.10+** and **PySpark 3.5+**
- **PostgreSQL 15+**

## Real-World Use Cases

- **Google** — PageRank over crawled web pages
- **Amazon** — "Customers also bought" via purchase history correlation
- **Netflix** — Video transcoding: chunk videos, process with FFmpeg in parallel, reassemble
- **Retail** — Market basket analysis for product placement optimization

## Contributing

Found an issue or want to add another batch framework comparison? Open an issue or submit a pull request.
