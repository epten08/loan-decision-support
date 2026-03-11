from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api import risk_router
from app.config import settings

app = FastAPI(
    title="Risk Inference Engine",
    description="ML-based risk scoring service for loan applications",
    version="1.0.0"
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.allowed_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routers
app.include_router(risk_router, prefix="/risk", tags=["Risk Assessment"])


@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {"status": "healthy", "service": "risk-engine"}


@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "service": "Risk Inference Engine",
        "version": "1.0.0",
        "status": "running"
    }
