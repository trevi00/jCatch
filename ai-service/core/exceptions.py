"""
커스텀 예외 클래스들

잡았다 AI 서비스에서 사용되는 모든 예외 클래스를 정의합니다.
계층적 예외 구조를 통해 체계적인 오류 처리와 로깅을 지원합니다.

예외 계층:
- JBDAIException: 모든 AI 서비스 예외의 최상위 클래스
  - OpenAIServiceException: OpenAI API 관련 예외
  - TranslationException: 번역 서비스 예외
  - InterviewException: AI 면접 서비스 예외
  - ChatbotException: 챗봇 서비스 예외
  - ImageGenerationException: 이미지 생성 서비스 예외
  - SentimentAnalysisException: 감정 분석 서비스 예외
  - ValidationException: 데이터 검증 예외
  - RateLimitException: API 요청 제한 예외
  - ConfigurationException: 설정 오류 예외

각 예외는 에러 코드와 상세 정보를 포함하여
디버깅과 모니터링을 용이하게 합니다.
"""

from typing import Any, Dict, Optional


class JBDAIException(Exception):
    """
    잡았다 AI 서비스의 모든 예외의 기본 클래스입니다.

    모든 커스텀 예외는 이 클래스를 상속받아 일관된 예외 처리
    구조를 제공합니다. 에러 코드와 상세 정보를 포함하여
    체계적인 오류 관리를 지원합니다.

    Attributes:
        message (str): 사용자에게 표시할 예외 메시지
        error_code (str): 프로그래밍적 오류 식별을 위한 코드
        details (Dict[str, Any]): 디버깅을 위한 추가 정보
    """

    def __init__(
        self,
        message: str,
        error_code: str = "UNKNOWN_ERROR",
        details: Optional[Dict[str, Any]] = None
    ):
        """
        JBDAIException을 초기화합니다.

        Args:
            message (str): 예외 메시지
            error_code (str): 에러 코드 (기본값: "UNKNOWN_ERROR")
            details (Optional[Dict[str, Any]]): 추가 정보 딕셔너리
        """
        super().__init__(message)
        self.message = message
        self.error_code = error_code
        self.details = details or {}


class OpenAIServiceException(JBDAIException):
    """
    OpenAI API 호출 관련 예외입니다.

    OpenAI API 요청 실패, 응답 파싱 오류, 토큰 한도 초과 등
    OpenAI 서비스와 관련된 모든 오류를 처리합니다.

    사용 예시:
    - API 키 오류
    - 모델 접근 권한 오류
    - 토큰 한도 초과
    - 네트워크 연결 오류
    """

    def __init__(self, message: str, details: Optional[Dict[str, Any]] = None):
        """
        OpenAI 서비스 예외를 초기화합니다.

        Args:
            message (str): 예외 메시지
            details (Optional[Dict[str, Any]]): 추가 오류 정보
        """
        super().__init__(message, "OPENAI_SERVICE_ERROR", details)


class TranslationException(JBDAIException):
    """
    번역 서비스 관련 예외입니다.

    텍스트 번역 처리 중 발생하는 모든 오류를 처리합니다.
    언어 감지 실패, 번역 품질 문제, 지원하지 않는 언어 등의
    상황에서 발생합니다.

    사용 예시:
    - 지원하지 않는 언어 쌍
    - 번역할 텍스트가 너무 긴 경우
    - 언어 감지 실패
    - 번역 API 오류
    """

    def __init__(self, message: str, details: Optional[Dict[str, Any]] = None):
        """
        번역 서비스 예외를 초기화합니다.

        Args:
            message (str): 예외 메시지
            details (Optional[Dict[str, Any]]): 번역 관련 추가 정보
        """
        super().__init__(message, "TRANSLATION_ERROR", details)

class ValidationException(JBDAIException):
    """
    데이터 검증 관련 예외입니다.

    API 요청 데이터 검증, 필드 유효성 검사, 타입 검증 등
    입력 데이터 검증 과정에서 발생하는 모든 오류를 처리합니다.

    사용 예시:
    - 필수 필드 누락
    - 잘못된 데이터 타입
    - 값 범위 초과
    - 형식 불일치
    """

    def __init__(self, message: str, field: str = "", details: Optional[Dict[str, Any]] = None):
        """
        데이터 검증 예외를 초기화합니다.

        Args:
            message (str): 예외 메시지
            field (str): 검증 실패한 필드명
            details (Optional[Dict[str, Any]]): 검증 관련 추가 정보
        """
        details = details or {}
        details["field"] = field
        super().__init__(message, "VALIDATION_ERROR", details)


class RateLimitException(JBDAIException):
    """
    API 요청 제한 관련 예외입니다.

    사용자 또는 IP별 API 호출 횟수 제한, 동시 접속 제한 등
    서비스 보호를 위한 제한 정책 위반 시 발생합니다.

    사용 예시:
    - 분당 요청 횟수 초과
    - 일일 사용량 한도 초과
    - OpenAI API 토큰 한도 초과
    - 동시 접속자 수 제한 초과
    """

    def __init__(self, message: str = "API 요청 제한에 도달했습니다", details: Optional[Dict[str, Any]] = None):
        """
        API 요청 제한 예외를 초기화합니다.

        Args:
            message (str): 예외 메시지 (기본값: "API 요청 제한에 도달했습니다")
            details (Optional[Dict[str, Any]]): 제한 관련 추가 정보
        """
        super().__init__(message, "RATE_LIMIT_ERROR", details)


class ConfigurationException(JBDAIException):
    """
    설정 오류 관련 예외입니다.

    환경 변수 누락, 설정 파일 오류, API 키 설정 문제 등
    애플리케이션 설정과 관련된 모든 오류를 처리합니다.

    사용 예시:
    - 필수 환경 변수 누락
    - 잘못된 API 키 형식
    - 설정 파일 파싱 오류
    - 데이터베이스 연결 설정 오류
    """

    def __init__(self, message: str, details: Optional[Dict[str, Any]] = None):
        """
        설정 오류 예외를 초기화합니다.

        Args:
            message (str): 예외 메시지
            details (Optional[Dict[str, Any]]): 설정 관련 추가 정보
        """
        super().__init__(message, "CONFIGURATION_ERROR", details)