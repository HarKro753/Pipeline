# AWS Cost & Resource Analysis — 1TB Daily Normalization

## Dataset Assumptions

| Metric                | Value                                  |
| --------------------- | -------------------------------------- |
| Raw CSV size          | 1 TB                                   |
| Rows (avg 1KB/row)    | ~1 billion                             |
| Columns               | 200+ (13 comma-separated to normalize) |
| Relations per product | ~15 avg (across 13 tables)             |
| Total relation rows   | ~15 billion                            |
| Schedule              | Daily cron at 3 AM UTC                 |

## Pipeline Stages & Timing

| Stage              | What happens                                              | Duration (estimated) |
| ------------------ | --------------------------------------------------------- | -------------------- |
| **Map**            | 8000 mappers parse CSV chunks (128MB each) from S3        | ~15 min              |
| **Shuffle & Sort** | Hadoop redistributes ~2TB of intermediate data by barcode | ~20 min              |
| **Reduce**         | 64 reducers batch-insert into Postgres (1000 rows/flush)  | ~45 min              |
| **Total**          | End to end                                                | **~80 min**          |

## Recommended AWS Architecture

```
┌─────────────────────────────┐
│  S3: foodfacts.csv (1 TB)   │
└──────────────┬──────────────┘
               │
┌──────────────▼──────────────┐
│  EMR Cluster (transient)    │
│  1x m5.xlarge  (master)     │
│  10x m5.2xlarge (core)      │
│  64 reducers                │
└──────────────┬──────────────┘
               │ JDBC batch inserts
┌──────────────▼──────────────┐
│  RDS PostgreSQL             │
│  db.r6g.2xlarge             │
│  500GB gp3 storage          │
└─────────────────────────────┘
```

## EMR Cluster Sizing

### Why 10x m5.2xlarge?

Each m5.2xlarge has 8 vCPUs and 32GB RAM. With 10 nodes:

- **80 vCPUs total** — enough for 8000 mapper tasks (Hadoop schedules them in waves)
- **320GB RAM total** — sufficient for shuffle/sort of ~2TB intermediate data
- **10 Gbps network** per node — handles shuffle traffic between map and reduce

### Master Node

| Component | Spec      | Reason                                        |
| --------- | --------- | --------------------------------------------- |
| Instance  | m5.xlarge | 4 vCPU, 16GB — runs NameNode, ResourceManager |
| EBS       | 100GB gp3 | HDFS metadata, job logs                       |

### Core Nodes (10x)

| Component       | Spec                  | Reason                                    |
| --------------- | --------------------- | ----------------------------------------- |
| Instance        | m5.2xlarge            | 8 vCPU, 32GB — runs DataNode, NodeManager |
| EBS             | 200GB gp3 each        | Local shuffle/spill storage               |
| YARN containers | 6 per node (5GB each) | Leaves headroom for OS + DataNode         |

### Map Phase

- 1TB / 128MB = **8192 InputSplits** → 8192 mapper tasks
- 60 YARN containers across cluster → ~137 waves of mappers
- Each mapper: parse CSV, emit (barcode, data) pairs
- S3 read throughput: 10 nodes × ~500MB/s = **5 GB/s** aggregate
- Map phase: ~15 minutes

### Reduce Phase

- 64 reducers (1 per YARN container, with headroom)
- Each reducer: reconstruct Product, batch-insert to Postgres
- 1B products / 64 reducers = ~15.6M products per reducer
- At 1000 rows/batch, ~15,600 batch flushes per reducer
- Reduce phase: ~45 minutes (bottlenecked by DB write throughput)

## RDS PostgreSQL Sizing

### Why db.r6g.2xlarge?

| Component       | Spec                                    | Reason                                       |
| --------------- | --------------------------------------- | -------------------------------------------- |
| Instance        | db.r6g.2xlarge                          | 8 vCPU, 64GB RAM                             |
| Storage         | 500GB gp3 (3000 IOPS, 125MB/s baseline) | Products + 13 relation tables                |
| Max connections | 64 (one per reducer)                    | Each reducer holds one persistent connection |

### Write Throughput

- 64 reducers × 1000 rows/batch = **64,000 rows per batch cycle**
- With `ON CONFLICT` upserts and batch inserts: ~50,000 rows/sec sustained
- Total writes: ~1B product inserts + ~15B relation inserts = **~16B rows**
- At 50K rows/sec: ~320,000 seconds = **too slow with a single RDS instance**

### The DB Bottleneck — Solutions

Option A: **Provisioned IOPS**

- Upgrade to gp3 with 16,000 IOPS → ~200K rows/sec → ~22 hours — still too slow

Option B: **Larger instance + more IOPS** (recommended)

- `db.r6g.4xlarge` (16 vCPU, 128GB) with io2 at 40,000 IOPS
- ~500K rows/sec sustained → **~9 hours** — fits in daily window but tight

Option C: **Disable indexes during load** (recommended, combine with B)

- Drop UNIQUE constraints and indexes before load
- Bulk insert without conflict checking
- Recreate indexes after load
- Speedup: **3-5x** → total write time ~2-3 hours

Option D: **COPY instead of INSERT** (optimal)

- Reducers write to S3 as CSV instead of direct DB inserts
- Post-job step: `COPY` from S3 into Postgres via `aws_s3` extension
- `COPY` throughput: ~2M rows/sec on r6g.4xlarge
- Total load: ~16B rows / 2M rows/sec = **~2.2 hours**
- Requires schema change: reducers output to S3, separate COPY step

### Recommended: Option C + B

| Component           | Spec                     | Monthly Cost   |
| ------------------- | ------------------------ | -------------- |
| RDS db.r6g.4xlarge  | 16 vCPU, 128GB, multi-AZ | ~$1,460        |
| 500GB io2, 40K IOPS | High-throughput storage  | ~$500          |
| **RDS Total**       |                          | **~$1,960/mo** |

## Full AWS Cost Breakdown

### EMR Cluster (transient, ~1.5 hrs/day)

| Resource          | Spec         | $/hr        | Daily hrs | Monthly (30d)  |
| ----------------- | ------------ | ----------- | --------- | -------------- |
| Master: m5.xlarge | 1 instance   | $0.192      | 1.5       | $8.64          |
| Core: m5.2xlarge  | 10 instances | $0.384 × 10 | 1.5       | $172.80        |
| EMR markup        | 25% of EC2   | ~$1.15/hr   | 1.5       | $51.84         |
| **EMR Total**     |              |             |           | **$233.28/mo** |

### Storage

| Resource               | Spec                               | Monthly     |
| ---------------------- | ---------------------------------- | ----------- |
| S3 input               | 1TB stored + 8192 GET requests/day | ~$25        |
| S3 intermediate output | ~100GB (cleaned daily)             | ~$3         |
| **Storage Total**      |                                    | **~$28/mo** |

### RDS PostgreSQL (always-on)

| Resource                | Spec           | Monthly        |
| ----------------------- | -------------- | -------------- |
| db.r6g.4xlarge multi-AZ | 16 vCPU, 128GB | $1,460         |
| 500GB io2 40K IOPS      |                | $500           |
| **RDS Total**           |                | **~$1,960/mo** |

### Data Transfer

| Resource               | Monthly |
| ---------------------- | ------- |
| S3 → EMR (same region) | Free    |
| EMR → RDS (same VPC)   | Free    |
| **Transfer Total**     | **$0**  |

## Monthly Total

| Component               | Monthly Cost   |
| ----------------------- | -------------- |
| EMR cluster (transient) | $233           |
| S3 storage              | $28            |
| RDS PostgreSQL          | $1,960         |
| **Total**               | **~$2,221/mo** |

## Optimization Opportunities

### 1. Use Spot Instances for EMR Core Nodes

- m5.2xlarge spot: ~$0.08/hr (vs $0.384 on-demand) = **80% savings**
- EMR core nodes with spot: $172.80 → ~$36/mo
- Risk: spot interruption mid-job (mitigated by Hadoop's fault tolerance)
- **Savings: ~$137/mo**

### 2. Use Aurora PostgreSQL Instead of RDS

- Aurora Serverless v2 scales to 128 ACU during load, back to 2 ACU idle
- Load period (3 hrs/day): ~64 ACU × $0.12/ACU-hr × 3 = $23/day
- Idle (21 hrs/day): ~2 ACU × $0.12/ACU-hr × 21 = $5/day
- Monthly: ~$840 vs $1,960 for always-on RDS
- **Savings: ~$1,120/mo**

### 3. Reduce EMR Cluster Size, Accept Longer Runtime

- 5x m5.2xlarge instead of 10 → ~2.5 hrs instead of 1.5 hrs
- Still fits in daily window, costs half
- **Savings: ~$86/mo**

## Optimized Monthly Total

| Component            | Cost         |
| -------------------- | ------------ |
| EMR (5 nodes, spot)  | $60          |
| S3                   | $28          |
| Aurora Serverless v2 | $840         |
| **Optimized Total**  | **~$928/mo** |

## Time Budget (24-hour cycle)

```
03:00  EMR cluster spins up (2 min)
03:02  MapReduce job starts
03:17  Map phase complete (8192 mappers)
03:37  Shuffle & Sort complete
04:22  Reduce phase complete (batch inserts to Postgres)
04:25  EMR cluster terminates
04:25  Index rebuild begins (if using Option C)
05:00  Index rebuild complete
05:00  Pipeline idle until next day
       ────────────────────────────
       Total wall time: ~2 hours
       Idle time: 22 hours
```
