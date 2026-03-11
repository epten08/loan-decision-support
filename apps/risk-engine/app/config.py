from pydantic_settings import BaseSettings
from typing import List


class Settings(BaseSettings):
    """Application settings"""

    app_name: str = "Risk Inference Engine"
    debug: bool = False

    # CORS settings
    allowed_origins: List[str] = ["http://localhost:8080", "http://localhost:3000"]

    # Model settings
    model_version: str = "heuristic-v1"

    class Config:
        env_file = ".env"


settings = Settings()
