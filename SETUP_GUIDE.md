# Loan Decision Support System - Setup & Deployment Guide

## Step 1: Push to GitHub

### 1.1 Create a GitHub Repository

1. Go to [github.com/new](https://github.com/new)
2. Repository name: `loan-decision-support`
3. Description: "Loan Decision Support System - Backend platform for evaluating loan applications"
4. Choose **Private** or **Public**
5. Do NOT initialize with README (we already have one)
6. Click **Create repository**

### 1.2 Configure Git and Push

```bash
# Navigate to project root
cd c:\Users\mviyo\projects\ps\loan-decision-support

# Check current git status
git status

# Add all files to staging
git add .

# Create initial commit (if not already done)
git commit -m "Initial project setup: Loan Decision Support System

- Spring Boot Decision API with rule engine and risk adapter
- FastAPI Risk Engine for ML-based scoring
- PostgreSQL database with Flyway migrations
- Nx monorepo configuration
- Docker Compose for local development

Co-Authored-By: Claude <noreply@anthropic.com>"

# Add remote origin (replace YOUR_USERNAME with your GitHub username)
git remote add origin https://github.com/YOUR_USERNAME/loan-decision-support.git

# Push to GitHub
git branch -M main
git push -u origin main
```

### 1.3 Verify .gitignore

Ensure these are in your `.gitignore`:

```gitignore
# Dependencies
node_modules/
.gradle/
build/
target/

# IDE
.idea/
*.iml
.vscode/
*.swp

# Environment
.env
.env.local
*.env

# Build outputs
dist/
out/
*.jar

# Python
__pycache__/
*.pyc
.venv/
venv/

# Logs
*.log
logs/

# OS
.DS_Store
Thumbs.db

# Nx
.nx/
```

---

## Step 2: Local Development Setup

### 2.1 Prerequisites

| Tool | Version | Installation |
|------|---------|--------------|
| Node.js | 18+ | [nodejs.org](https://nodejs.org) |
| Java JDK | 21 | [adoptium.net](https://adoptium.net) |
| Python | 3.11+ | [python.org](https://python.org) |
| Docker | Latest | [docker.com](https://docker.com) |

### 2.2 Clone and Install

```bash
# Clone the repository
git clone https://github.com/YOUR_USERNAME/loan-decision-support.git
cd loan-decision-support

# Install Node.js dependencies
npm install

# Install Python dependencies
cd apps/risk-engine
python -m venv venv
venv\Scripts\activate  # Windows
# source venv/bin/activate  # Linux/Mac
pip install -r requirements.txt
cd ../..
```

### 2.3 Start Services

```bash
# Terminal 1: Start PostgreSQL
docker-compose up postgres -d

# Terminal 2: Start Decision API
npx nx serve decision-api

# Terminal 3: Start Risk Engine
npx nx serve risk-engine
```

### 2.4 Verify Services

| Service | URL | Health Check |
|---------|-----|--------------|
| Decision API | http://localhost:8080 | http://localhost:8080/actuator/health |
| Risk Engine | http://localhost:8001 | http://localhost:8001/health |
| PostgreSQL | localhost:5433 | `docker exec loan-postgres pg_isready` |

---

## Step 3: Test the API

### 3.1 Submit a Loan Application

```bash
curl -X POST http://localhost:8080/api/loan-applications \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "nationalId": "123456789",
    "dateOfBirth": "1985-06-15",
    "employmentStatus": "EMPLOYED",
    "requestedAmount": 25000,
    "termMonths": 36,
    "loanPurpose": "HOME_IMPROVEMENT",
    "monthlyIncome": 5000,
    "monthlyExpenses": 2000
  }'
```

### 3.2 Evaluate the Application

```bash
# Replace {id} with the UUID from the previous response
curl -X POST http://localhost:8080/api/loan-applications/{id}/evaluate
```

### 3.3 Get Decision

```bash
curl http://localhost:8080/api/loan-applications/{id}/decision
```

---

## Step 4: Project Structure Reference

```
loan-decision-support/
├── apps/
│   ├── decision-api/           # Spring Boot (Java 21)
│   │   ├── src/main/java/com/loan/decision/
│   │   │   ├── loanintake/     # Loan application intake
│   │   │   ├── creditprofile/  # Credit profile management
│   │   │   ├── rules/          # Rule engine
│   │   │   ├── riskadapter/    # Risk engine client
│   │   │   ├── decisioning/    # Decision aggregation
│   │   │   └── governance/     # Audit logging
│   │   ├── src/main/resources/
│   │   │   ├── db/migration/   # Flyway migrations
│   │   │   ├── rules/          # Rule YAML files
│   │   │   └── application.properties
│   │   ├── build.gradle
│   │   └── Dockerfile
│   │
│   └── risk-engine/            # FastAPI (Python 3.11)
│       ├── app/
│       │   ├── main.py         # FastAPI app
│       │   ├── api.py          # Endpoints
│       │   ├── models.py       # Pydantic models
│       │   ├── config.py       # Settings
│       │   └── risk_model.py   # Risk scoring
│       ├── requirements.txt
│       └── Dockerfile
│
├── libs/
│   └── rule-definitions/       # Shared rule YAML files
│
├── docker-compose.yml          # Service orchestration
├── nx.json                     # Nx configuration
├── package.json                # Root dependencies
└── README.md                   # Project documentation
```

---

## Step 5: Nx Commands Reference

```bash
# Build Decision API
npx nx build decision-api

# Run Decision API
npx nx serve decision-api

# Test Decision API
npx nx test decision-api

# Run Risk Engine
npx nx serve risk-engine

# Run all services
npx nx run-many -t serve --projects=decision-api,risk-engine

# View dependency graph
npx nx graph

# List all projects
npx nx show projects
```

---

## Step 6: Database Management

### View Tables

```bash
docker exec -it loan-postgres psql -U postgres -d loandb -c "\dt"
```

### Run SQL Queries

```bash
docker exec -it loan-postgres psql -U postgres -d loandb
```

### Reset Database

```bash
docker-compose down -v
docker-compose up postgres -d
```

---

## Step 7: Environment Configuration

### Decision API (`apps/decision-api/src/main/resources/application.properties`)

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5433/loandb
spring.datasource.username=postgres
spring.datasource.password=postgres

# Risk Engine
risk-engine.base-url=http://localhost:8001
```

### Risk Engine (`apps/risk-engine/.env`)

```env
DEBUG=false
```

---

## Step 8: Troubleshooting

### Port Already in Use

```bash
# Find process using port 8080
netstat -ano | findstr :8080

# Kill process (replace PID)
taskkill /F /PID <PID>
```

### Database Connection Failed

```bash
# Check if PostgreSQL is running
docker ps

# Check logs
docker logs loan-postgres

# Restart PostgreSQL
docker-compose restart postgres
```

### Gradle Build Failed

```bash
# Clean and rebuild
cd apps/decision-api
./gradlew clean build
```

---

## Step 9: API Documentation

Once the Decision API is running, access Swagger UI at:

**http://localhost:8080/swagger-ui.html**

---

## Step 10: Next Steps

1. **Add CI/CD**: Create GitHub Actions workflow for automated testing
2. **Add Authentication**: Implement JWT or OAuth2
3. **Add Monitoring**: Integrate Prometheus + Grafana
4. **Deploy to Cloud**: Use Docker Compose on cloud VM or Kubernetes
5. **Train ML Model**: Replace heuristic risk model with trained model
