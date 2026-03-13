from decimal import Decimal
from typing import Tuple
import numpy as np

from app.models import RiskAssessmentRequest
from app.config import settings


class RiskModel:
    """
    Heuristic-based risk model for probability of default calculation.
    In production, this would be replaced with an ML model (e.g., LightGBM, XGBoost).
    """

    def __init__(self):
        self.model_version = settings.model_version
        self.model_type = "heuristic"

    def predict(self, request: RiskAssessmentRequest) -> Tuple[float, str, float]:
        """
        Calculate probability of default and risk band.

        Returns:
            Tuple of (probability_of_default, risk_band, confidence)
        """
        # Extract features with defaults
        credit_score = request.credit_score or 600
        dti = float(request.debt_to_income_ratio or 30)
        missed_payments = request.missed_payments_last_12_months or 0
        defaults = request.defaults_last_5_years or 0
        credit_history = request.credit_history_months or 24
        existing_loans = request.existing_loan_count or 0

        # Calculate base PD from credit score
        pd = self._credit_score_to_pd(credit_score)

        # Adjust for DTI
        pd = self._adjust_for_dti(pd, dti)

        # Adjust for payment history
        pd = self._adjust_for_payment_history(pd, missed_payments, defaults)

        # Adjust for credit history length
        pd = self._adjust_for_credit_history(pd, credit_history)

        # Adjust for loan burden
        pd = self._adjust_for_loan_burden(pd, existing_loans, request)

        # Clamp PD between 0.01 and 0.99
        pd = max(0.01, min(0.99, pd))

        # Determine risk band
        risk_band = self._pd_to_risk_band(pd)

        # Calculate confidence (higher for more complete data)
        confidence = self._calculate_confidence(request)

        return pd, risk_band, confidence

    def _credit_score_to_pd(self, credit_score: int) -> float:
        """Convert credit score to base probability of default"""
        if credit_score >= 750:
            return 0.02
        elif credit_score >= 700:
            return 0.05
        elif credit_score >= 650:
            return 0.10
        elif credit_score >= 600:
            return 0.18
        elif credit_score >= 550:
            return 0.28
        elif credit_score >= 500:
            return 0.40
        else:
            return 0.55

    def _adjust_for_dti(self, pd: float, dti: float) -> float:
        """Adjust PD based on debt-to-income ratio"""
        if dti <= 20:
            return pd * 0.85
        elif dti <= 30:
            return pd * 0.95
        elif dti <= 40:
            return pd * 1.05
        elif dti <= 50:
            return pd * 1.20
        else:
            return pd * 1.50

    def _adjust_for_payment_history(self, pd: float, missed: int, defaults: int) -> float:
        """Adjust PD based on payment history"""
        if defaults > 0:
            pd = pd * (1 + 0.5 * defaults)
        if missed > 0:
            pd = pd * (1 + 0.1 * missed)
        return pd

    def _adjust_for_credit_history(self, pd: float, months: int) -> float:
        """Adjust PD based on credit history length"""
        if months >= 120:  # 10+ years
            return pd * 0.85
        elif months >= 60:  # 5+ years
            return pd * 0.92
        elif months >= 24:  # 2+ years
            return pd * 1.0
        elif months >= 12:  # 1+ year
            return pd * 1.15
        else:
            return pd * 1.30

    def _adjust_for_loan_burden(self, pd: float, existing_loans: int,
                                 request: RiskAssessmentRequest) -> float:
        """Adjust PD based on existing loan burden"""
        if existing_loans == 0:
            return pd * 0.95
        elif existing_loans <= 2:
            return pd * 1.0
        elif existing_loans <= 5:
            return pd * 1.10
        else:
            return pd * 1.25

    def _pd_to_risk_band(self, pd: float) -> str:
        """Convert probability of default to risk band"""
        if pd <= 0.05:
            return "A"
        elif pd <= 0.10:
            return "B"
        elif pd <= 0.20:
            return "C"
        elif pd <= 0.35:
            return "D"
        else:
            return "E"

    def _calculate_confidence(self, request: RiskAssessmentRequest) -> float:
        """Calculate model confidence based on data completeness"""
        total_fields = 12
        present_fields = sum([
            request.credit_score is not None,
            request.debt_to_income_ratio is not None,
            request.existing_loan_count is not None,
            request.total_existing_debt is not None,
            request.credit_history_months is not None,
            request.missed_payments_last_12_months is not None,
            request.defaults_last_5_years is not None,
            request.employment_status is not None,
            request.monthly_income is not None,
            request.monthly_expenses is not None,
            request.requested_amount is not None,
            request.term_months is not None,
        ])

        # Base confidence from data completeness
        completeness = present_fields / total_fields

        # Higher confidence with more critical fields
        critical_present = sum([
            request.credit_score is not None,
            request.debt_to_income_ratio is not None,
            request.defaults_last_5_years is not None,
        ])
        critical_bonus = critical_present * 0.05

        return min(0.98, 0.70 + (completeness * 0.20) + critical_bonus)


# Singleton instance
risk_model = RiskModel()
