import pytest
from fastapi.testclient import TestClient
from app.main import app


client = TestClient(app)


def test_health_check():
    """Test health endpoint"""
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "healthy"


def test_root():
    """Test root endpoint"""
    response = client.get("/")
    assert response.status_code == 200
    assert response.json()["service"] == "Risk Inference Engine"


def test_risk_assess_basic():
    """Test basic risk assessment"""
    request_data = {
        "monthly_income": 5000,
        "requested_amount": 25000,
        "term_months": 36,
        "credit_score": 700
    }
    response = client.post("/risk/assess", json=request_data)
    assert response.status_code == 200
    data = response.json()
    assert "pd" in data
    assert "risk_band" in data
    assert "confidence" in data
    assert data["risk_band"] in ["A", "B", "C", "D", "E"]


def test_risk_assess_high_risk():
    """Test risk assessment for high-risk applicant"""
    request_data = {
        "monthly_income": 2000,
        "requested_amount": 50000,
        "term_months": 60,
        "credit_score": 450,
        "debt_to_income_ratio": 60,
        "defaults_last_5_years": 1
    }
    response = client.post("/risk/assess", json=request_data)
    assert response.status_code == 200
    data = response.json()
    assert data["risk_band"] in ["D", "E"]


def test_model_info():
    """Test model info endpoint"""
    response = client.get("/risk/model-info")
    assert response.status_code == 200
    data = response.json()
    assert "model_version" in data
    assert "risk_bands" in data
