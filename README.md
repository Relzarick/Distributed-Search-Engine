# Distributed Search Engine

Visit the site at: https://search-engine.relzarick.workers.dev/

## Setup

Clone the repo into your linux file system (There will be a big time penalty otherwise) and
place the dataset into the data folder under the backend dir.

The system is designed to only ingest a single .csv file and
does not support multiple datasets currently.

- The .csv file may be deleted after the first boot.

### Dataset Used

- https://www.kaggle.com/datasets/alanvourch/tmdb-movies-daily-updates?resource=download

## Libraries Used

| Library                   | Version                                                                                                                                                                     |
| ------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **fastutil**              | [![fastutil](https://img.shields.io/badge/8.5.18-20639B?logoColor=white)](https://mvnrepository.com/artifact/it.unimi.dsi/fastutil/8.5.18)                                  |
| **fastdoubleparser**      | [![fastdoubleparser](https://img.shields.io/badge/2.0.1-007ACC?logoColor=white)](https://mvnrepository.com/artifact/ch.randelshofer/fastdoubleparser/2.0.1)                 |
| **uuidv7**                | [![uuidv7](https://img.shields.io/badge/1.1.0-6F42C1?logoColor=white)](https://mvnrepository.com/artifact/io.github.robsonkades/uuidv7/1.1.0)                               |
| **opennlp-tools**         | [![opennlp-tools](https://img.shields.io/badge/3.0.0--M4-D22128?logo=apache&logoColor=white)](https://mvnrepository.com/artifact/org.apache.opennlp/opennlp-tools/3.0.0-M4) |
| **fastcsv**               | [![fastcsv](https://img.shields.io/badge/4.3.1-107C41?logoColor=white)](https://mvnrepository.com/artifact/de.siegmar/fastcsv/4.3.1)                                        |
| **mongodb-driver-sync**   | [![mongodb-driver-sync](https://img.shields.io/badge/5.8.0-47A248?logo=mongodb&logoColor=white)](https://mvnrepository.com/artifact/org.mongodb/mongodb-driver-sync/5.8.0)  |
| **lettuce-core**          | [![lettuce-core](https://img.shields.io/badge/7.6.0.RELEASE-DC382D?logo=redis&logoColor=white)](https://mvnrepository.com/artifact/io.lettuce/lettuce-core/7.6.0.RELEASE)   |
| **junit-jupiter-api**     | [![junit-jupiter-api](https://img.shields.io/badge/6.1.0-25A162?logo=junit5&logoColor=white)](https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api/6.1.0) |
| **mockito-junit-jupiter** | [![mockito-junit-jupiter](https://img.shields.io/badge/5.23.0-78A641?logoColor=white)](https://mvnrepository.com/artifact/org.mockito/mockito-junit-jupiter/5.23.0)         |
| **logback-classic**       | [![logback-classic](https://img.shields.io/badge/1.5.38-000000?logoColor=white)](https://mvnrepository.com/artifact/ch.qos.logback/logback-classic/1.5.38)                  |

## How It Works

The ingestion pipeline runs in two steps: indexing, and parsing & insertion. (outdated benchmarks)

### Step One: Indexing

After staging the file onto the named volume,
the program then indexes the entire document to prepare for multithreaded parsing.

- It averages around 2.5s.

### Step Two: Parsing & Insertion

This is where the program parses through the files and inserts them into MongoDB.
Using a producer and consumer pattern with a LinkedBlockingQueue,
the pure Mongo throughput hits around 270k RPS (rows per second).

- Averages ~8.5s at around 200k RPS.

---

## Performance Benchmarks

| Metric                   | Throughput |
| ------------------------ | ---------- |
| **Total Pipeline**       | ~166k RPS  |
| **Parsing & Processing** | ~196k RPS  |
| **Pure Mongo Operation** | ~270k RPS  |
