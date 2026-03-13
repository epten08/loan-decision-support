from fastapi import APIRouter, HTTPException
import logging

from app.models import RiskAssessmentRequest, RiskAssessmentResponse
from app.risk_model import risk_model

logger = logging.getLogger(__name__)

risk_router = APIRouter()


@risk_router.post("/assess", response_model=RiskAssessmentResponse)
async def assess_risk(request: RiskAssessmentRequest) -> RiskAssessmentResponse:
    """
    Assess the risk of a loan application.

    This endpoint receives financial features of a loan application
    and returns a probability of default score along with a risk band classification.
    """
    try:
        logger.info(f"Received risk assessment request: income={request.monthly_income}, "
                   f"amount={request.requested_amount}, credit_score={request.credit_score}")

        # Get prediction from model
        pd, risk_band, confidence = risk_model.predict(request)

        response = RiskAssessmentResponse(
            pd=round(pd, 6),
            risk_band=risk_band,
            confidence=round(confidence, 4),
            model_version=risk_model.model_version,
            model_type=risk_model.model_type
        )

        logger.info(f"Risk assessment result: PD={pd:.4f}, Band={risk_band}, "
                   f"Confidence={confidence:.2f}")

        return response

    except Exception as e:
        logger.error(f"Error in risk assessment: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Risk assessment failed: {str(e)}")


@risk_router.get("/model-info")
async def get_model_info():
    """Get information about the current risk model"""
    return {
        "model_version": risk_model.model_version,
        "model_type": risk_model.model_type,
        "description": "Heuristic-based risk scoring model",
        "risk_bands": {
            "A": "Very low risk (PD <= 5%)",
            "B": "Low risk (5% < PD <= 10%)",
            "C": "Medium risk (10% < PD <= 20%)",
            "D": "High risk (20% < PD <= 35%)",
            "E": "Very high risk (PD > 35%)"
        }
    }
