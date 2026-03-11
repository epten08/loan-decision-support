# Loan Decision Support System

A backend platform for evaluating loan applications using deterministic rule evaluation, machine learning risk inference, decision aggregation logic, and full audit traceability.

## Project Overview

This is an Nx monorepo containing two main services:

1. **Decision API** (Spring Boot) - Responsible for rule evaluation and decisioning
2. **Risk Inference Engine** (FastAPI) - Responsible for probability-of-default scoring

## Repository Structure

```
loan-decision-support/
├── apps/
│   ├── decision-api/          # Spring Boot decision service
│   └── risk-engine/           # FastAPI risk scoring service
├── libs/
│   ├── domain-models/         # Shared domain models
│   ├── decision-contracts/    # API contracts
│   └── rule-definitions/      # Rule YAML definitions
├── infra/
│   └── docker/                # Docker configurations
├── nx.json                    # Nx workspace configuration
├── package.json               # Root package configuration
└── docker-compose.yml         # Service orchestration
```

## Prerequisites

- Node.js 18+
- Java 21 (JDK)
- Python 3.11+
- Docker & Docker Compose
- Gradle (wrapper included)

## Installation

### 1. Install Node dependencies

```bash
npm install
```

### 2. Install Python dependencies (Risk Engine)

```bash
cd apps/risk-engine
pip install -r requirements.txt
```

### 3. Build Decision API

```bash
npx nx build decision-api
```

## Running the Services

### Using Nx Commands

Start the Decision API:
```bash
npx nx serve decision-api
```

Start the Risk Engine:
```bash
npx nx serve risk-engine
```

Run both services:
```bash
npx nx run-many -t serve --all
```

### Using Docker Compose

Start all services (PostgreSQL, Decision API, Risk Engine):
```bash
docker-compose up -d
```

Stop all services:
```bash
docker-compose down
```

## Service Endpoints

| Service | Port | Base URL |
|---------|------|----------|
| Decision API | 8080 | http://localhost:8080 |
| Risk Engine | 8001 | http://localhost:8001 |
| PostgreSQL | 5432 | localhost:5432 |

## API Endpoints

### Decision API

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/loan-applications` | Submit a new loan application |
| GET | `/api/loan-applications/{id}` | Get application details |
| POST | `/api/loan-applications/{id}/evaluate` | Trigger evaluation |
| GET | `/api/loan-applications/{id}/decision` | Get decision result |

### Risk Engine

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/risk/assess` | Assess risk for application |
| GET | `/risk/model-info` | Get model information |
| GET | `/health` | Health check |

## Testing

Run Decision API tests:
```bash
npx nx test decision-api
```

Run Risk Engine tests:
```bash
npx nx test risk-engine
```

## Development

### View Dependency Graph

```bash
npx nx graph
```

### Build All Projects

```bash
npx nx run-many -t build --all
```

## Technology Stack

- **Decision API**: Java 21, Spring Boot 3.2, Spring Data JPA, Flyway, PostgreSQL
- **Risk Engine**: Python 3.11, FastAPI, Pydantic, scikit-learn
- **Infrastructure**: Docker, Docker Compose, Nx
- **Database**: PostgreSQL 16

## License

MIT
