from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from datetime import datetime
from enum import Enum

# 면접 관련 스키마
class InterviewType(str, Enum):
    TECHNICAL = "technical"
    PERSONALITY = "personality"

# 번역 관련 스키마
class DocumentType(str, Enum):
    RESUME = "resume"
    COVER_LETTER = "cover_letter"
    EMAIL = "email"
    BUSINESS = "business"
    GENERAL = "general"

class TranslationRequest(BaseModel):
    text: str
    source_language: str = "ko"
    target_language: str = "en"
    document_type: DocumentType = DocumentType.GENERAL

class TranslationResponse(BaseModel):
    original_text: str
    translated_text: str
    source_language: str
    target_language: str
    document_type: DocumentType
    created_at: datetime

# 번역 품질 평가 관련 스키마
class TranslationEvaluationRequest(BaseModel):
    original: str
    translated: str
    source_language: str = "ko"
    target_language: str = "en"

# 공통 응답 스키마
class APIResponse(BaseModel):
    success: bool
    message: str
    data: Optional[Any] = None
    error: Optional[str] = None