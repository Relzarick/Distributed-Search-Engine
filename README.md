# Distributed Search Engine

Simple engine.

---

## Setup

Place your dataset into the data folder under the backend dir.

The system is designed to only ingest a single .csv file and
does not support multiple datasets currently.
(tested up to 1.6M records)

- The .csv file may be deleted after the first boot.

### Dataset Used

- https://www.kaggle.com/datasets/alanvourch/tmdb-movies-daily-updates?resource=download

### Libraries Used

| Library             | Version                                                                                                                                                                     |
|---------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| gson                | [![gson](https://img.shields.io/badge/2.14.0-4285F4?logo=google&logoColor=white)](https://mvnrepository.com/artifact/com.google.code.gson/gson/2.14.0)                      |
| mongodb-driver-sync | [![mongodb-driver-sync](https://img.shields.io/badge/5.8.0-47A248?logo=mongodb&logoColor=white)](https://mvnrepository.com/artifact/org.mongodb/mongodb-driver-sync/5.8.0)  |
| lettuce-core        | [![lettuce-core](https://img.shields.io/badge/7.6.0.RELEASE-DC382D?logo=redis&logoColor=white)](https://mvnrepository.com/artifact/io.lettuce/lettuce-core/7.6.0.RELEASE)   |
| commons-csv         | [![commons-csv](https://img.shields.io/badge/1.14.1-D22128?logo=apache&logoColor=white)](https://mvnrepository.com/artifact/org.apache.commons/commons-csv/1.14.1)          |
| junit-jupiter-api   | [![junit-jupiter-api](https://img.shields.io/badge/6.1.0-25A162?logo=junit5&logoColor=white)](https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api/6.1.0) |
| mockito-core        | [![mockito-core](https://img.shields.io/badge/5.23.0-78A641&logoColor=white)](https://mvnrepository.com/artifact/org.mockito/mockito-core/5.23.0)                           |

---

## How It Works

On startup, the program scans the data folder for CSV files,
validating against a set of restrictions before being passed to the parser.

The parser performs basic type conversion and returns an iterator list of documents.

This iterator is handed off as tasks to virtual threads for concurrent execution.
Each task first writes its document to MongoDB, then passes that task to the indexer.

Inside the indexer, the tokenizer interface cleans up the document's fields.
Only the internal MongoDB ID and valid text fields are then written to Redis.

---

## Running The App

Open the terminal and run the following commands.

- On first start-up to create docker image:

```bash
docker compose up -d --build
```

- Subsequent runs:

```bash
docker compose up -d
```