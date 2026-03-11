# Loan Decision Support System - Architecture

## System Overview

The Loan Decision Support System is a backend platform designed to evaluate loan applications through a combination of deterministic rule evaluation and machine learning-based risk assessment.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                           Client/UI                                  │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      Decision API (Spring Boot)                      │
│                          Port: 8080                                  │
├─────────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────────┐  │
│  │  Loan Intake │  │   Credit     │  │     Decisioning          │  │
│  │   Module     │  │   Profile    │  │       Module             │  │
│  └──────────────┘  └──────────────┘  └──────────────────────────┘  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────────┐  │
│  │    Rules     │  │    Risk      │  │     Governance           │  │
│  │   Engine     │  │   Adapter    │  │    (Audit Logs)          │  │
│  └──────────────┘  └──────────────┘  └──────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
         │                    │                      │
         │                    │                      │
         ▼                    ▼                      ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────┐
│   PostgreSQL    │  │  Risk Engine   │  │    Rule Definitions     │
│   Database      │  │   (FastAPI)    │  │       (YAML)            │
│   Port: 5432    │  │   Port: 8001   │  │                         │
└─────────────────┘  └─────────────────┘  └─────────────────────────┘
```

## Components

### 1. Decision API (Spring Boot)

The main backend service responsible for orchestrating the loan decision process.

#### Modules

| Module | Purpose |
|--------|---------|
| **loanintake** | Handles loan application submission and applicant management |
| **creditprofile** | Captures and manages credit profile snapshots |
| **rules** | Deterministic rule engine for evaluating business rules |
| **riskadapter** | Integrates with the Risk Engine for ML-based scoring |
| **decisioning** | Aggregates rule results and risk scores into final decisions |
| **governance** | Provides audit logging and decision traceability |

#### Package Structure

```
com.loan.decision/
├── DecisionApiApplication.java
├── loanintake/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   └── model/
├── creditprofile/
│   ├── service/
│   ├── repository/
│   └── model/
├── rules/
│   ├── service/
│   ├── repository/
│   └── model/
├── riskadapter/
│   ├── controller/dto/
│   ├── service/
│   ├── repository/
│   └── model/
├── decisioning/
│   ├── controller/dto/
│   ├── service/
│   └── model/
└── governance/
    ├── service/
    ├── repository/
    └── model/
```

### 2. Risk Engine (FastAPI)

A Python-based microservice that provides ML-based probability of default (PD) scoring.

#### Components

- **Risk Model**: Heuristic-based scoring model (can be replaced with ML model)
- **API Layer**: FastAPI endpoints for risk assessment
- **Configuration**: Environment-based settings

### 3. Database (PostgreSQL)

Stores all application data with full audit traceability.

#### Tables

| Table | Purpose |
|-------|---------|
| `applicants` | Applicant personal information |
| `loan_applications` | Loan application details |
| `credit_profiles` | Credit profile snapshots (immutable) |
| `rule_evaluations` | Individual rule evaluation results |
| `risk_assessments` | Risk scoring results |
| `decisions` | Final decision outcomes |
| `decision_audit_logs` | Complete audit trail |

## Decision Flow

```
1. Application Submission
   └── POST /api/loan-applications
       └── Creates Applicant (if new)
       └── Creates LoanApplication (PENDING)

2. Evaluation Trigger
   └── POST /api/loan-applications/{id}/evaluate
       └── Status → UNDER_REVIEW
       └── Capture Credit Profile
       └── Execute Rule Engine
       └── Call Risk Engine
       └── Aggregate Decision
       └── Create Audit Log
       └── Status → APPROVED/DECLINED/CONDITIONAL

3. Decision Retrieval
   └── GET /api/loan-applications/{id}/decision
       └── Returns DecisionResponse
```

## Decision Logic

### Rule Evaluation

Rules are defined in YAML files with two severity levels:

- **HARD**: Automatic decline if failed
- **SOFT**: Warning, may still approve

### Risk Band Classification

| Band | PD Range | Interpretation |
|------|----------|----------------|
| A | 0-5% | Very low risk |
| B | 5-10% | Low risk |
| C | 10-20% | Medium risk |
| D | 20-35% | High risk |
| E | >35% | Very high risk |

### Decision Aggregation

```
IF any HARD rule fails:
    → DECLINE

ELSE IF risk_band IN (A, B):
    → APPROVE

ELSE IF risk_band = C:
    → CONDITIONAL

ELSE:
    → DECLINE
```

## Data Flow

### Request/Response Example

**Submit Application:**
```json
POST /api/loan-applications
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "nationalId": "123456789",
  "requestedAmount": 25000,
  "termMonths": 36,
  "loanPurpose": "HOME_IMPROVEMENT",
  "monthlyIncome": 5000,
  "monthlyExpenses": 2000
}
```

**Evaluate & Get Decision:**
```json
POST /api/loan-applications/{id}/evaluate

Response:
{
  "applicationId": "uuid",
  "outcome": "APPROVED",
  "reasonCodes": [],
  "riskBand": "B",
  "hardRuleFailures": 0,
  "softRuleFailures": 0,
  "summary": "Approved with risk band B",
  "decidedAt": "2024-01-15T10:30:00"
}
```

## Security Considerations

1. **Data Protection**: Sensitive applicant data is stored securely
2. **Audit Trail**: All decisions are logged with full context
3. **Immutability**: Credit profiles and decisions cannot be modified
4. **Input Validation**: All inputs are validated at the API layer

## Scalability

- **Stateless Services**: Both APIs are stateless and horizontally scalable
- **Database**: PostgreSQL supports read replicas for scaling reads
- **Async Processing**: Risk engine calls can be made asynchronous if needed

## Future Enhancements

1. **ML Model Integration**: Replace heuristic model with trained ML model
2. **SHAP Explanations**: Add explainability for risk predictions
3. **Kubernetes Deployment**: Add K8s manifests for production deployment
4. **Event Sourcing**: Implement event-driven architecture for better auditability
