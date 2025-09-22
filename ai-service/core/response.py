"""
API 응답 형식 표준화

잡았다 AI 서비스의 모든 API 응답을 위한 표준 모델과 유틸리티를 제공합니다.
일관된 응답 구조를 통해 클라이언트와의 통신을 체계화하고 에러 처리를 표준화합니다.

제공하는 응답 모델:
- APIResponse: 모든 API의 공통 응답 형식
- SentimentAnalysisResponse: 감정 분석 결과
- TranslationResponse: 번역 결과
- InterviewResponse: AI 면접 관련 응답
- ChatbotResponse: 챗봇 대화 응답
- ImageGenerationResponse: 이미지 생성 결과

모든 응답은 Pydantic 모델로 정의되어 자동 검증과 직렬화를 지원합니다.
"""

from typing import Any, Dict, Optional, Union
from pydantic import BaseModel


class APIResponse(BaseModel):
    """
    모든 API 응답의 표준 형식을 정의하는 기본 모델입니다.

    이 모델은 성공과 실패를 구분하고 일관된 구조로 데이터를 전달하여
    클라이언트에서 예측 가능한 응답 처리를 가능하게 합니다.

    Attributes:
        success (bool): 요청 처리 성공 여부
        message (str): 사용자에게 표시할 메시지
        data (Optional[Any]): 실제 응답 데이터 (성공 시)
        error_code (Optional[str]): 에러 식별을 위한 코드 (실패 시)
        details (Optional[Dict[str, Any]]): 추가 정보 또는 디버깅 정보

    Example:
        성공 응답:
        {
            "success": true,
            "message": "데이터를 성공적으로 조회했습니다",
            "data": {"id": 1, "name": "example"}
        }

        에러 응답:
        {
            "success": false,
            "message": "잘못된 요청입니다",
            "error_code": "VALIDATION_ERROR",
            "details": {"field": "email", "reason": "invalid format"}
        }
    """
    success: bool
    message: str
    data: Optional[Any] = None
    error_code: Optional[str] = None
    details: Optional[Dict[str, Any]] = None

    @classmethod
    def success_response(
        cls,
        data: Any = None,
        message: str = "성공적으로 처리되었습니다"
    ) -> "APIResponse":
        """
        성공 응답을 생성합니다.

        Args:
            data (Any): 클라이언트에게 전달할 데이터
            message (str): 성공 메시지 (기본값: "성공적으로 처리되었습니다")

        Returns:
            APIResponse: 성공 응답 객체

        Example:
            >>> response = APIResponse.success_response(
            ...     data={"users": [...]},
            ...     message="사용자 목록을 조회했습니다"
            ... )
        """
        return cls(
            success=True,
            message=message,
            data=data
        )

    @classmethod
    def error_response(
        cls,
        message: str,
        error_code: str = "UNKNOWN_ERROR",
        details: Optional[Dict[str, Any]] = None
    ) -> "APIResponse":
        """
        에러 응답을 생성합니다.

        Args:
            message (str): 사용자에게 표시할 에러 메시지
            error_code (str): 에러 식별 코드 (기본값: "UNKNOWN_ERROR")
            details (Optional[Dict[str, Any]]): 추가 에러 정보

        Returns:
            APIResponse: 에러 응답 객체

        Example:
            >>> response = APIResponse.error_response(
            ...     message="사용자를 찾을 수 없습니다",
            ...     error_code="USER_NOT_FOUND",
            ...     details={"user_id": 123}
            ... )
        """
        return cls(
            success=False,
            message=message,
            error_code=error_code,
            details=details
        )

class TranslationResponse(BaseModel):
    """
    번역 API의 응답 모델입니다.

    텍스트 번역 결과와 관련 메타데이터를 제공하여
    번역 품질과 언어 정보를 함께 전달합니다.

    Attributes:
        source_language (str): 원본 텍스트 언어 코드
        target_language (str): 번역 대상 언어 코드
        original_text (str): 원본 텍스트
        translated_text (str): 번역된 텍스트
        confidence (Optional[float]): 번역 신뢰도 (0.0 ~ 1.0)

    Example:
        {
            "source_language": "ko",
            "target_language": "en",
            "original_text": "안녕하세요",
            "translated_text": "Hello",
            "confidence": 0.98
        }
    """
    source_language: str
    target_language: str
    original_text: str
    translated_text: str
    confidence: Optional[float] = None