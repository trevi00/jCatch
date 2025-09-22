from openai import OpenAI
from typing import List, Dict, Any
from config.settings import settings
import logging

logger = logging.getLogger(__name__)

class OpenAIService:
    def __init__(self):
        self.client = OpenAI(api_key=settings.OPENAI_API_KEY)
    
    async def generate_completion(
        self, 
        messages: List[Dict[str, str]], 
        model: str = None,
        temperature: float = None,
        max_tokens: int = None
    ) -> str:
        """OpenAI Chat Completion API 호출"""
        try:
            response = self.client.chat.completions.create(
                model=model or settings.OPENAI_MODEL,
                messages=messages,
                temperature=temperature or settings.OPENAI_TEMPERATURE,
                max_tokens=max_tokens or settings.OPENAI_MAX_TOKENS
            )
            return response.choices[0].message.content
        except Exception as e:
            logger.error(f"OpenAI API 호출 실패: {e}")
            raise
    
    async def generate_image(
        self, 
        prompt: str, 
        size: str = None,
        quality: str = None,
        model: str = None
    ) -> str:
        """OpenAI Image Generation API 호출"""
        try:
            response = self.client.images.generate(
                model=model or settings.IMAGE_MODEL,
                prompt=prompt,
                size=size or settings.IMAGE_SIZE,
                quality=quality or settings.IMAGE_QUALITY,
                n=1,
            )
            return response.data[0].url
        except Exception as e:
            logger.error(f"OpenAI 이미지 생성 실패: {e}")
            raise
    
    async def create_embedding(self, text: str) -> List[float]:
        """OpenAI Embedding API 호출"""
        try:
            response = self.client.embeddings.create(
                model="text-embedding-ada-002",
                input=text
            )
            return response.data[0].embedding
        except Exception as e:
            logger.error(f"OpenAI 임베딩 생성 실패: {e}")
            raise

# 싱글톤 인스턴스
openai_service = OpenAIService()