# MapReduce vs Apache Spark — Detailed Comparison

## Execution Model

| Aspect                   | Hadoop MapReduce                      | Apache Spark                                    |
| ------------------------ | ------------------------------------- | ----------------------------------------------- |
| **Paradigm**             | Map → Shuffle & Sort → Reduce         | DAG (Directed Acyclic Graph)                    |
| **Intermediate Storage** | Disk (HDFS after each stage)          | In-memory (RAM), spills to disk                 |
| **Speed**                | Slower due to disk I/O between stages | 10–100x faster for iterative workloads          |
| **Fault Tolerance**      | Re-runs failed tasks from disk        | Recomputes lost partitions via lineage          |
| **API**                  | Verbose Java classes (Mapper/Reducer) | Concise DSL (Python, Scala, Java, R)            |
| **Streaming**            | Batch only                            | Structured Streaming (micro-batch + continuous) |

## When to Use What

| Use Case                             | MapReduce                     | Spark                       |
| ------------------------------------ | ----------------------------- | --------------------------- |
| Simple one-pass ETL                  | Good                          | Good                        |
| Iterative algorithms (ML, PageRank)  | Poor (disk I/O per iteration) | Excellent (in-memory)       |
| Interactive queries                  | Not suitable                  | Good (Spark SQL)            |
| Stream processing                    | Not suitable                  | Good (Structured Streaming) |
| Very large datasets with limited RAM | Good (disk-based)             | Needs tuning                |

## Real-World Use Cases

- **Google**: PageRank computation over crawled web pages using MapReduce
- **Amazon**: "Customers also bought" — correlate purchase histories across products
- **Netflix**: Transcode video into multiple bitrates by chunking and processing with FFmpeg
- **Retail**: Market basket analysis to identify product affinities

## Modern Landscape

Most organizations today use higher-level abstractions rather than raw MapReduce:

- **Apache Spark** — the de facto successor, DAG-based, in-memory
- **Google BigQuery** — serverless SQL over petabytes
- **AWS Athena** — serverless SQL over S3
- **Apache Flink** — true streaming with batch capabilities
