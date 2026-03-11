1. Project Overview

The Loan Decision Support System is a backend platform designed to evaluate loan applications using a hybrid approach combining:

deterministic rule evaluation

probabilistic risk inference

auditable decision aggregation

The system will assist financial institutions in evaluating loan applications and producing explainable decisions.

The MVP focuses on:

loan application intake

rule-based eligibility checks

machine-learning-based risk scoring

final decision generation

full audit logging

query-based decision retrieval

The system will be developed as a polyglot monorepo managed with Nx.

Primary services:

Decision API (Spring Boot)

Risk Inference Engine (FastAPI)

Query UI (future)

Shared contracts and rules

2. Core Objectives

The system must:

evaluate loan applications automatically

produce explainable decisions

maintain immutable decision history

support deterministic rule evaluation

integrate machine learning risk assessment

support regulatory auditability

3. High-Level Architecture
Client/UI
   |
   v
Decision API (Spring Boot)
   |
   |---- Rules Engine
   |
   |---- Risk Engine Adapter
   |        |
   |        v
   |   Risk Inference Service (FastAPI)
   |
   v
Decision Aggregator
   |
   v
PostgreSQL (Audit + Decision Storage)
4. Technology Stack
Repository Management

Nx Monorepo

Node.js

npm

Decision API

Language
Java 17

Framework
Spring Boot

Libraries

Spring Web

Spring Validation

Spring Data JPA

Flyway

Lombok

MapStruct

Responsibilities

loan application management

rule evaluation

communication with risk engine

decision aggregation

audit logging

API exposure

Risk Inference Service

Language
Python 3.11+

Framework
FastAPI

Libraries

FastAPI

Uvicorn

Pydantic

Scikit-learn

LightGBM (optional)

SHAP (future)

Responsibilities

receive application features

run trained risk model

return probability of default

return risk band classification

Database

PostgreSQL 16

Responsibilities

application storage

credit snapshots

rule evaluation results

risk assessment results

final decisions

audit logs

Infrastructure

Docker
Docker Compose

Future

Kubernetes

Terraform

CI/CD via GitHub Actions

5. Core Domain Concepts
Applicant

Represents an individual applying for a loan.

Attributes may include:

national ID

employment status

monthly income

demographic information

Loan Application

Represents a request for a loan.

Attributes include:

requested amount

loan tenure

product type

applicant reference

Credit Profile

A snapshot of the applicant's credit state at evaluation time.

Contains:

credit score

active loans

outstanding debt

defaults

Risk Assessment

Represents the machine learning evaluation result.

Includes:

probability of default

risk band

model version

confidence score

Rule Evaluation

Represents the evaluation of eligibility or compliance rules.

Each rule contains:

rule code

result (pass or fail)

severity (hard or soft)

reason code

Decision

The final outcome produced by the system.

Possible outcomes:

APPROVE

DECLINE

CONDITIONAL

A decision must include reason codes explaining the outcome.

Decisions are immutable.

6. Functional Requirements
Loan Application Submission

The system must allow creation of loan applications containing:

applicant data

loan request details

Credit Profile Capture

The system must capture a credit profile snapshot during evaluation.

This snapshot must remain immutable.

Rule Evaluation

The system must evaluate deterministic rules including:

minimum income thresholds

credit default checks

debt-to-income ratios

regulatory compliance rules

Rules must produce:

pass/fail result

severity level

reason codes

Risk Assessment

The system must request risk scoring from the inference engine.

Inputs include:

financial indicators

credit indicators

Outputs include:

probability of default

risk band

Decision Aggregation

The system must combine:

rule results

risk assessment

To generate a final decision.

Example logic:

hard rule failure results in decline

low risk band results in approval

medium risk band results in conditional approval

Decision Persistence

Every decision must be stored with:

decision outcome

reasons

model version

timestamp

Decisions must not be editable.

Decision Query

The system must allow retrieval of decisions by:

application ID

applicant ID

Audit Logging

The system must log:

rule evaluations

risk assessment results

final decisions

Logs must be immutable.

7. Non-Functional Requirements
Explainability

Every decision must provide reason codes explaining the outcome.

Auditability

All evaluations must be traceable including:

rule version

model version

evaluation timestamp

Reliability

The system must handle failure of the risk engine gracefully.

Fallback behavior may include:

conditional decisions

manual review flags

Performance

Evaluation of a single application should complete within:

2 seconds under normal conditions.

Security

Sensitive applicant data must be protected through:

encrypted transport (TLS)

role-based API access

restricted database access

8. API Requirements
Submit Loan Application

POST /api/loan-applications

Evaluate Loan Application

POST /api/loan-applications/{id}/evaluate

Retrieve Decision

GET /api/loan-applications/{id}/decision

9. Data Persistence Requirements

The database must maintain tables for:

applicants

loan applications

credit profiles

risk assessments

rule evaluations

decisions

decision audit logs

10. Future Enhancements

Future system capabilities may include:

credit bureau integration

automated document verification

rule simulation environment

model retraining pipelines

multi-tenant financial institutions

loan portfolio risk analytics

regulatory reporting