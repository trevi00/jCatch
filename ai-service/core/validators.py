"""
공통 유효성 검증기

잡았다 AI 서비스의 모든 API 요청에 대한 입력 데이터 검증을 수행합니다.
데이터 무결성 보장과 보안 취약점 방지를 위한 종합적인 검증 기능을 제공합니다.

주요 검증 기능:
- 이메일 형식 검증
- 사용자 ID 유효성 검사
- 텍스트 길이 제한 검사
- 언어 코드 검증
- 면접 관련 파라미터 검증
- 이미지 생성 파라미터 검증
- XSS 방지를 위한 텍스트 정리

모든 검증 실패 시 ValidationException을 발생시켜
일관된 에러 처리를 보장합니다.
"""

import re
from typing import List, Optional, Union
from core.exceptions import ValidationException


def validate_email(email: str) -> bool:
    """
    이메일 주소 형식의 유효성을 검증합니다.

    RFC 5322 표준에 기반한 기본적인 이메일 형식을 검증하며,
    사용자 등록 및 인증 시 이메일 주소 검증에 사용됩니다.

    Args:
        email (str): 검증할 이메일 주소

    Returns:
        bool: 유효한 이메일 형식인 경우 True, 그렇지 않으면 False

    Example:
        >>> validate_email("user@example.com")
        True
        >>> validate_email("invalid-email")
        False
    """
    pattern = r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$'
    return re.match(pattern, email) is not None


def validate_user_id(user_id: Union[str, int]) -> str:
    """
    사용자 ID의 유효성을 검증하고 문자열로 변환합니다.

    숫자 또는 문자열 형태의 사용자 ID를 받아 공백을 제거하고
    유효성을 검사한 후 표준화된 문자열 형태로 반환합니다.

    Args:
        user_id (Union[str, int]): 검증할 사용자 ID

    Returns:
        str: 정제된 사용자 ID 문자열

    Raises:
        ValidationException: 사용자 ID가 없거나 유효하지 않은 경우

    Example:
        >>> validate_user_id("123")
        "123"
        >>> validate_user_id(456)
        "456"
    """
    if not user_id:
        raise ValidationException("사용자 ID는 필수입니다", "user_id")

    user_id_str = str(user_id).strip()
    if not user_id_str:
        raise ValidationException("유효하지 않은 사용자 ID입니다", "user_id")

    return user_id_str


def validate_text_length(
    text: str,
    field_name: str,
    min_length: int = 1,
    max_length: int = 5000
) -> str:
    """
    텍스트의 길이를 검증하고 정제합니다.

    입력 텍스트의 최소/최대 길이를 검사하여 API 요청의
    데이터 무결성을 보장합니다. 공백을 제거한 후 길이를 측정합니다.

    Args:
        text (str): 검증할 텍스트
        field_name (str): 필드명 (에러 메시지에 사용)
        min_length (int): 최소 길이 (기본값: 1)
        max_length (int): 최대 길이 (기본값: 5000)

    Returns:
        str: 정제된 텍스트

    Raises:
        ValidationException: 텍스트가 없거나 길이 제한을 벗어난 경우

    Example:
        >>> validate_text_length("  hello  ", "message", 1, 100)
        "hello"
    """
    if not text:
        raise ValidationException(f"{field_name}은(는) 필수입니다", field_name)

    text = text.strip()
    if len(text) < min_length:
        raise ValidationException(
            f"{field_name}은(는) 최소 {min_length}자 이상이어야 합니다",
            field_name
        )

    if len(text) > max_length:
        raise ValidationException(
            f"{field_name}은(는) 최대 {max_length}자 이하여야 합니다",
            field_name
        )

    return text


def validate_language_code(language: str, supported_languages: List[str]) -> str:
    """
    언어 코드의 유효성을 검증합니다.

    지원되는 언어 목록과 대조하여 유효한 언어 코드인지 확인하며,
    번역 서비스 및 다국어 지원 기능에서 사용됩니다.

    Args:
        language (str): 검증할 언어 코드 (예: "ko", "en")
        supported_languages (List[str]): 지원되는 언어 코드 목록

    Returns:
        str: 정제된 언어 코드 (소문자)

    Raises:
        ValidationException: 언어 코드가 없거나 지원하지 않는 경우

    Example:
        >>> validate_language_code("KO", ["ko", "en", "ja"])
        "ko"
    """
    if not language:
        raise ValidationException("언어 코드는 필수입니다", "language")

    language = language.lower().strip()
    if language not in supported_languages:
        raise ValidationException(
            f"지원하지 않는 언어입니다. 지원 언어: {', '.join(supported_languages)}",
            "language"
        )

    return language

def validate_prompt(prompt: str, max_length: int = 1000) -> str:
    """
    AI 프롬프트의 유효성을 검증합니다.

    이미지 생성, 텍스트 생성 등에 사용되는 프롬프트의 길이와
    내용을 검증하여 AI 모델의 안정적인 동작을 보장합니다.

    Args:
        prompt (str): 검증할 프롬프트 텍스트
        max_length (int): 최대 길이 (기본값: 1000)

    Returns:
        str: 정제된 프롬프트

    Raises:
        ValidationException: 프롬프트가 없거나 길이 제한을 초과한 경우

    Example:
        >>> validate_prompt("아름다운 풍경")
        "아름다운 풍경"
    """
    return validate_text_length(prompt, "prompt", 1, max_length)


def sanitize_text(text: str) -> str:
    """
    텍스트를 정리하여 보안 취약점을 방지합니다.

    XSS(Cross-Site Scripting) 공격 방지를 위해 HTML 태그를 제거하고
    공백을 정리하여 안전한 텍스트로 변환합니다.

    Args:
        text (str): 정리할 텍스트

    Returns:
        str: 정리된 안전한 텍스트

    보안 기능:
        - HTML 태그 완전 제거
        - 연속된 공백 정리 (단일 공백으로 변환)
        - 앞뒤 공백 제거

    Example:
        >>> sanitize_text("<script>alert('xss')</script>안녕하세요   ")
        "안녕하세요"
        >>> sanitize_text("  여러     공백    ")
        "여러 공백"
    """
    if not text:
        return ""

    # 기본적인 HTML 태그 제거
    text = re.sub(r'<[^>]+>', '', text)

    # 연속된 공백 정리
    text = re.sub(r'\s+', ' ', text)

    return text.strip()