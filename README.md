# Intelligence Query Engine - HNG Stage 2

A powerful search engine for demographic profile data, featuring advanced dynamic filtering and a rule-based Natural Language Query (NLQ) parser.

## Features
- **Advanced Query Engine**: Build complex queries using filters like `min_age`, `gender`, `country_id`, and confidence probabilities.
- **Natural Language Parsing**: Search using plain English like "young males from nigeria" or "females above 30".
- **Pagination & Sorting**: Efficiently manage large datasets with customizable sorting and page-based results.
- **Persistent Storage**: Pre-seeded with 2026 idempotent profile records.
- **Standards Compliant**: Uses UUID v7 for time-ordered IDs and ISO 8601 UTC timestamps.

## Prerequisites
- **Java 21** or higher.
- **Maven 3.x** (or use the included wrapper).

## Local Setup

### 1. Run the Engine
```bash
cd hng14/stage2/backend
./mvnw spring-boot:run
```
The server will start at `http://localhost:9090`.

## API Documentation

### 1. Advanced Filtering
`GET /api/profiles`
- **Params**: `gender`, `age_group`, `country_id`, `min_age`, `max_age`, `min_gender_probability`, `min_country_probability`.
- **Pagination**: `page` (default 1), `limit` (default 10, max 50).
- **Sorting**: `sort_by` (age, created_at, gender_probability), `order` (asc, desc).

### 2. Natural Language Search
`GET /api/profiles/search?q={query}`
- **Examples**:
  - `q=young males` (Maps to male, age 16-24)
  - `q=females above 30` (Maps to female, min_age 30)
  - `q=adult males from kenya` (Maps to male, age_group adult, country KE)

## Database
- **H2 Console**: `http://localhost:9090/h2-console`
- **JDBC URL**: `jdbc:h2:mem:querydb`
