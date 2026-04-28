# Batch Processing Pipeline — MapReduce vs Apache Spark

A side-by-side comparison of batch processing approaches using real-world **OpenFoodFacts** data: classic **Hadoop MapReduce** (Java, DDD architecture) vs **Apache Spark** (Python).

## Highlights

- Hands-on comparison of MapReduce and Spark with identical use cases
- Java MapReduce project structured with **Domain-Driven Design** (Domain, Application, Infrastructure layers)
- Real-world normalization of OpenFoodFacts CSV (200+ columns, N:M tag relations)
- PostgreSQL persistence with connection via `.env`
- Classic word count example in both frameworks
- Detailed [comparison document](docs/comparison.md) with code snippets and trade-offs

## Architecture

```
mapreduce-ddd/
├── domain/                  # Pure business logic, no framework deps
│   ├── model/               # Product (aggregate root), Tag, ProductTag
│   ├── valueobject/         # Barcode, NutriScore, NutrientInfo
│   └── repository/          # Interfaces only
├── application/             # Use case orchestration, DTOs
│   ├── service/             # NormalizationService, WordCountService
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

### MapReduce Phases

```
Input (CSV) → Map (extract key-value pairs) → Shuffle & Sort (group by key) → Reduce (aggregate) → Output
```

1. **Map** — Each worker takes a row and emits `(key, value)` pairs (e.g., `(tag, barcode)`)
2. **Shuffle & Sort** — The framework groups all values by key across the cluster
3. **Reduce** — Each worker receives a key and all its values, producing the final result

## Comparison at a Glance

|                          | MapReduce                 | Spark                             |
| ------------------------ | ------------------------- | --------------------------------- |
| **Language**             | Java                      | Python                            |
| **Intermediate storage** | Disk (HDFS)               | In-memory (RAM)                   |
| **Word count**           | ~40 lines                 | ~5 lines                          |
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

# Run word count
hadoop jar target/mapreduce-ddd-1.0.0.jar \
  com.pipeline.application.service.WordCountService \
  input/text.txt output/wordcount

# Run OpenFoodFacts normalization (writes to Postgres)
hadoop jar target/mapreduce-ddd-1.0.0.jar \
  com.pipeline.application.service.NormalizationService \
  input/foodfacts.csv output/normalized
```

### Spark (Python)

```bash
cd spark
pip install -r requirements.txt

# Word count
spark-submit word_count.py

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
    brand TEXT,
    nutriscore_score INT,
    nutriscore_grade VARCHAR(1),
    energy_kcal DOUBLE PRECISION,
    fat DOUBLE PRECISION,
    saturated_fat DOUBLE PRECISION,
    sugars DOUBLE PRECISION,
    salt DOUBLE PRECISION,
    proteins DOUBLE PRECISION,
    fiber DOUBLE PRECISION
);

CREATE TABLE tags (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL
);

CREATE TABLE product_tags (
    product_barcode VARCHAR(14) REFERENCES products(barcode),
    tag_id BIGINT REFERENCES tags(id),
    PRIMARY KEY (product_barcode, tag_id)
);
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
