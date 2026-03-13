from pydantic import BaseModel, Field
from typing import Optional
from decimal import Decimal


class RiskAssessmentRequest(BaseModel):
    """Request model for risk assessment"""

    monthly_income: Decimal = Field(..., gt=0, description="Monthly income")
    monthly_expenses: Optional[Decimal] = Field(None, ge=0, description="Monthly expenses")
    requested_amount: Decimal = Field(..., gt=0, description="Requested loan amount")
    term_months: int = Field(..., gt=0, le=360, description="Loan term in months")
    credit_score: Optional[int] = Field(None, ge=300, le=850, description="Credit score")
    debt_to_income_ratio: Optional[Decimal] = Field(None, ge=0, le=100, description="DTI ratio percentage")
    existing_loan_count: Optional[int] = Field(None, ge=0, description="Number of existing loans")
    total_existing_debt: Optional[Decimal] = Field(None, ge=0, description="Total existing debt")
    credit_history_months: Optional[int] = Field(None, ge=0, description="Credit history in months")
    missed_payments_last_12_months: Optional[int] = Field(None, ge=0, description="Missed payments last 12 months")
    defaults_last_5_years: Optional[int] = Field(None, ge=0, description="Defaults in last 5 years")
    employment_status: Optional[str] = Field(None, description="Employment status")

    class Config:
        json_schema_extra = {
            "example": {
                "monthly_income": 5000,
                "monthly_expenses": 2000,
                "requested_amount": 25000,
                "term_months": 36,
                "credit_score": 700,
                "debt_to_income_ratio": 25,
                "existing_loan_count": 1,
                "total_existing_debt": 5000,
                "credit_history_months": 60,
                "missed_payments_last_12_months": 0,
                "defaults_last_5_years": 0,
                "employment_status": "EMPLOYED"
            }
        }


class RiskAssessmentResponse(BaseModel):
    """Response model for risk assessment"""

    pd: float = Field(..., ge=0, le=1, description="Probability of default (0-1)")
    risk_band: str = Field(..., description="Risk band classification (A-E)")
    confidence: float = Field(..., ge=0, le=1, description="Model confidence (0-1)")
    model_version: str = Field(..., description="Model version used for assessment")
    model_type: str = Field(..., description="Type of model used (e.g., logistic_regression, heuristic)")

    class Config:
        json_schema_extra = {
            "example": {
                "pd": 0.04,
                "risk_band": "B",
                "confidence": 0.92,
                "model_version": "heuristic-v1",
                "model_type": "heuristic"
            }
        }
