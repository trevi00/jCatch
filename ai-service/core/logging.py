"""
로깅 설정 및 유틸리티

잡았다 AI 서비스의 전역 로깅 시스템을 제공합니다.
구조화된 로깅을 통해 애플리케이션 모니터링과 디버깅을 지원합니다.

주요 기능:
- 다중 로거 설정 (애플리케이션, 서비스, API별)
- 콘솔 및 파일 로깅 지원
- 구조화된 로그 포맷
- 에러 추적 및 컨텍스트 정보 포함

로거 종류:
- app_logger: 애플리케이션 전반적인 로그
- service_logger: 서비스 계층 로그
- api_logger: API 요청/응답 로그
"""

import logging
import sys
from pathlib import Path
from typing import Optional

# 로그 디렉토리 생성
LOG_DIR = Path("logs")
LOG_DIR.mkdir(exist_ok=True)


def setup_logger(
    name: str,
    level: int = logging.INFO,
    log_file: Optional[str] = None
) -> logging.Logger:
    """
    커스텀 로거를 설정합니다.

    지정된 이름과 설정으로 로거를 생성하고 콘솔 및 파일 핸들러를 추가합니다.
    중복 핸들러 추가를 방지하여 로그 중복을 막습니다.

    Args:
        name (str): 로거 이름 (예: "jbd.service")
        level (int): 로그 레벨 (기본값: logging.INFO)
        log_file (Optional[str]): 로그 파일명 (지정 시 파일 로깅 활성화)

    Returns:
        logging.Logger: 설정된 로거 인스턴스

    Example:
        >>> logger = setup_logger("my_service", logging.DEBUG, "debug.log")
        >>> logger.info("서비스 시작됨")
    """
    logger = logging.getLogger(name)

    # 이미 핸들러가 있으면 반환 (중복 설정 방지)
    if logger.handlers:
        return logger

    logger.setLevel(level)

    # 포맷터 설정 (시간, 로거명, 레벨, 메시지 포함)
    formatter = logging.Formatter(
        '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )

    # 콘솔 핸들러 (표준 출력)
    console_handler = logging.StreamHandler(sys.stdout)
    console_handler.setFormatter(formatter)
    logger.addHandler(console_handler)

    # 파일 핸들러 (선택적)
    if log_file:
        file_handler = logging.FileHandler(LOG_DIR / log_file, encoding='utf-8')
        file_handler.setFormatter(formatter)
        logger.addHandler(file_handler)

    return logger


# 전역 로거 인스턴스들
app_logger = setup_logger("jbd.app", log_file="app.log")
service_logger = setup_logger("jbd.service", log_file="service.log")
api_logger = setup_logger("jbd.api", log_file="api.log")


def log_api_call(endpoint: str, method: str, user_id: Optional[str] = None):
    """
    API 호출을 로깅합니다.

    REST API 엔드포인트 호출 정보를 기록하여 API 사용 패턴과
    사용자별 접근 현황을 모니터링할 수 있습니다.

    Args:
        endpoint (str): API 엔드포인트 경로
        method (str): HTTP 메서드 (GET, POST 등)
        user_id (Optional[str]): 사용자 ID (없으면 "Anonymous")

    Example:
        >>> log_api_call("/api/v1/interview/generate", "POST", "user123")
    """
    api_logger.info(f"{method} {endpoint} - User: {user_id or 'Anonymous'}")


def log_service_call(service: str, method: str, **kwargs):
    """
    서비스 계층 메서드 호출을 로깅합니다.

    비즈니스 로직 계층의 메서드 실행을 추적하여
    서비스 흐름과 매개변수를 모니터링합니다.

    Args:
        service (str): 서비스 클래스명
        method (str): 호출된 메서드명
        **kwargs: 메서드 매개변수들

    Example:
        >>> log_service_call("InterviewService", "generate_questions",
        ...                  field="backend", level="junior")
    """
    service_logger.info(f"{service}.{method} - {kwargs}")


def log_error(error: Exception, context: str = ""):
    """
    예외 발생을 로깅합니다.

    예외 정보와 스택 트레이스를 포함하여 상세한 에러 로그를 기록합니다.
    컨텍스트 정보를 추가하여 에러 발생 위치를 명확히 합니다.

    Args:
        error (Exception): 발생한 예외 객체
        context (str): 에러 발생 컨텍스트 (함수명, 모듈명 등)

    Example:
        >>> try:
        ...     risky_operation()
        ... except Exception as e:
        ...     log_error(e, "interview_service.generate_questions")
    """
    app_logger.error(f"Error in {context}: {str(error)}", exc_info=True)