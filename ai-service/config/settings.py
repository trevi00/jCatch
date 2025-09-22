import os
from dotenv import load_dotenv

load_dotenv()

class Settings:
    OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
    FASTAPI_HOST = os.getenv("FASTAPI_HOST", "localhost")
    FASTAPI_PORT = int(os.getenv("FASTAPI_PORT", 8001))
    SPRING_BOOT_URL = os.getenv("SPRING_BOOT_URL", "http://localhost:8080")
    LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")
    
    # OpenAI 설정
    OPENAI_MODEL = "gpt-4-turbo-preview"  # GPT-4로 업그레이드
    OPENAI_MAX_TOKENS = 4000  # 더 긴 응답 가능
    OPENAI_TEMPERATURE = 0.7
    
    # 추가 모델 설정
    OPENAI_MODEL_FAST = "gpt-3.5-turbo"  # 빠른 응답용
    OPENAI_MODEL_ADVANCED = "gpt-4-turbo-preview"  # 고급 작업용
    
    # RAG 설정
    EMBEDDING_MODEL = "sentence-transformers/all-MiniLM-L6-v2"
    VECTOR_DB_PATH = "./data/vector_store"
    CHUNK_SIZE = 500
    CHUNK_OVERLAP = 50
    
    # 이미지 생성 설정
    IMAGE_MODEL = "dall-e-3"
    IMAGE_SIZE = "1024x1024"
    IMAGE_QUALITY = "standard"

settings = Settings()