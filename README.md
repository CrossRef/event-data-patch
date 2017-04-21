# Event Data Patch

A tool designed to be used interactively. Patches Events in the Event Bus storage and uploads them. For infrequent use.

## Storage

Uses Mongo. Three collections: `original` is fetched from S3. `working` is the working set. On ingestion, data is read into both collections, then `working` is successively edited in-place by functions in the `event-data-patch.edit` namespace.

## Usage

Use from the REPL, in the `event-data-patch.core` namespace. Make sure your date range is represented as the `EPOCH_START` and `EPOCH_END` environment variables.

 - `(clear-storage)` - start a session off by clearing out all data.
 - `(ingest-all-epoch)` - pull all events in the given date range
 - `(verify-all-epoch)` - verify that we got them all


## Config

All compulsory.

| Environment variable | Description                                |
|----------------------|--------------------------------------------|
| `MONGODB_URI`        | Connection URI for Mongo                   |
| `S3_KEY`             |                                            |
| `S3_SECRET`          |                                            |
| `S3_BUCKET_NAME`     | Bucket where Event Bus stores data         |
| `S3_REGION_NAME`     |                                            |
| `EPOCH_START`        | YYYY-MM-DD first date we are interested in |
| `EPOCH_END`          | YYYY-MM-DD last date we are interested in  |