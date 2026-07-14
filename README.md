# Distributed Search Engine

Search engine for csv documents.

---

## Setup

Place your dataset into the data folder under the backend dir.

The system is designed to only ingest a single .csv file and
does not support multiple datasets currently.

- The .csv file may be deleted after the first boot.

### Dataset Used

- https://www.kaggle.com/datasets/alanvourch/tmdb-movies-daily-updates?resource=download

### Libraries Used

| Library             | Version                                                                                                                                                                     |
|---------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| gson                | [![gson](https://img.shields.io/badge/2.14.0-4285F4?logo=google&logoColor=white)](https://mvnrepository.com/artifact/com.google.code.gson/gson/2.14.0)                      |
| mongodb-driver-sync | [![mongodb-driver-sync](https://img.shields.io/badge/5.8.0-47A248?logo=mongodb&logoColor=white)](https://mvnrepository.com/artifact/org.mongodb/mongodb-driver-sync/5.8.0)  |
| lettuce-core        | [![lettuce-core](https://img.shields.io/badge/7.6.0.RELEASE-DC382D?logo=redis&logoColor=white)](https://mvnrepository.com/artifact/io.lettuce/lettuce-core/7.6.0.RELEASE)   |
| fastcsv             | [![fastcsv](https://img.shields.io/badge/4.3.1-107C41?logoColor=white)](https://mvnrepository.com/artifact/de.siegmar/fastcsv/4.3.1)                                        |
| junit-jupiter-api   | [![junit-jupiter-api](https://img.shields.io/badge/6.1.0-25A162?logo=junit5&logoColor=white)](https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api/6.1.0) |
| mockito-core        | [![mockito-core](https://img.shields.io/badge/5.23.0-78A641&logoColor=white)](https://mvnrepository.com/artifact/org.mockito/mockito-core/5.23.0)                           |

---

## How It Works

The ingestion pipeline runs in three stages: steps, indexing, and parsing and insertion.

### Step One: Staging

The program first stages the CSV file into a Docker named volume from a mount.
The reason behind this, instead of just using the file directly from the mount,
is to minimize the I/O overhead between Docker and the host OS.
I also did not want to give the user an additional step of
manually creating a named volume and copying the files to it beforehand.
This means the user can still easily replace the file without creating a new volume.

* This process averages around 1.5s, but saves ~10 seconds in total.

### Step Two: Indexing

After staging the file onto the named volume,
the program then indexes the entire document to prepare for multithreaded parsing.

* It averages around 2.5s.

### Step Three: Parsing & Insertion

This is where the program parses through the files and inserts them into MongoDB.
Using a producer and consumer pattern with a LinkedBlockingQueue,
the pure Mongo throughput hits around 270k RPS (rows per second).

* Averages ~8.5s at around 200k RPS.

---

## Performance Benchmarks

| Metric                   | Throughput |
|--------------------------|------------|
| **Total Pipeline**       | ~166k RPS  |
| **Parsing & Processing** | ~196k RPS  |
| **Pure Mongo Operation** | ~270k RPS  |