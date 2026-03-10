from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    ollama_base_url: str = "http://localhost:11434"
    ollama_model: str = "llama3.2"          # change to any pulled model e.g. mistral, phi3
    ollama_timeout_seconds: int = 120
    service_host: str = "0.0.0.0"
    service_port: int = 8000
    log_level: str = "info"

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


settings = Settings()
