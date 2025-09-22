"""
공통 미들웨어

잡았다 AI 서비스의 HTTP 요청 처리를 위한 미들웨어 모음입니다.
요청/응답 처리 파이프라인에서 횡단 관심사를 처리합니다.

제공 미들웨어:
- ExceptionHandlerMiddleware: 전역 예외 처리
- RequestLoggingMiddleware: 요청/응답 로깅
- CORSMiddleware: CORS 헤더 처리
- SecurityMiddleware: 보안 헤더 추가

미들웨어 실행 순서:
1. SecurityMiddleware (보안 헤더)
2. CORSMiddleware (CORS 처리)
3. RequestLoggingMiddleware (로깅)
4. ExceptionHandlerMiddleware (예외 처리)
"""

import time
import uuid
from typing import Callable
from fastapi import Request, Response, HTTPException
from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware

from core.exceptions import JBDAIException
from core.response import APIResponse
from core.logging import api_logger, log_error


class ExceptionHandlerMiddleware(BaseHTTPMiddleware):
    """
    전역 예외 처리 미들웨어입니다.

    애플리케이션에서 발생하는 모든 예외를 중앙 집중식으로 처리하여
    일관된 에러 응답 형태를 클라이언트에게 제공합니다.

    처리하는 예외 유형:
    - JBDAIException: 커스텀 비즈니스 예외
    - HTTPException: FastAPI HTTP 예외
    - Exception: 예상치 못한 일반 예외

    모든 예외는 APIResponse 형태로 구조화되어 반환됩니다.
    """

    async def dispatch(self, request: Request, call_next: Callable) -> Response:
        """
        요청을 처리하고 발생하는 예외를 캐치하여 적절한 응답을 반환합니다.

        Args:
            request (Request): FastAPI 요청 객체
            call_next (Callable): 다음 미들웨어 또는 핸들러

        Returns:
            Response: 정상 응답 또는 에러 응답
        """
        try:
            response = await call_next(request)
            return response

        except JBDAIException as e:
            # 커스텀 예외 처리 - 비즈니스 로직 오류
            log_error(e, f"API: {request.url.path}")
            return JSONResponse(
                status_code=400,
                content=APIResponse.error_response(
                    message=e.message,
                    error_code=e.error_code,
                    details=e.details
                ).dict()
            )

        except HTTPException as e:
            # FastAPI HTTP 예외 처리 - 표준 HTTP 오류
            return JSONResponse(
                status_code=e.status_code,
                content=APIResponse.error_response(
                    message=str(e.detail),
                    error_code="HTTP_ERROR"
                ).dict()
            )

        except Exception as e:
            # 예상치 못한 예외 처리 - 서버 내부 오류
            log_error(e, f"Unexpected error in API: {request.url.path}")
            return JSONResponse(
                status_code=500,
                content=APIResponse.error_response(
                    message="서버 내부 오류가 발생했습니다",
                    error_code="INTERNAL_SERVER_ERROR"
                ).dict()
            )


class RequestLoggingMiddleware(BaseHTTPMiddleware):
    """
    HTTP 요청/응답 로깅 미들웨어입니다.

    모든 HTTP 요청과 응답을 추적하여 API 사용 패턴을 모니터링하고
    성능 분석을 위한 상세한 로그를 제공합니다.

    주요 기능:
    - 요청별 고유 ID 생성 및 추적
    - 요청 시작 시간 기록
    - 처리 시간 측정 및 로깅
    - 클라이언트 IP 주소 기록
    - 응답 상태 코드 로깅
    - 응답 헤더에 요청 ID와 처리 시간 추가

    로그 포맷:
    - 요청: [request_id] METHOD /path - Client: ip_address
    - 응답: [request_id] Completed in 0.123s - Status: 200
    - 에러: [request_id] Failed in 0.456s - Error: error_message

    이 미들웨어는 디버깅과 성능 모니터링에 필수적이며,
    프로덕션 환경에서 API 동작을 추적하는 데 사용됩니다.
    """

    async def dispatch(self, request: Request, call_next: Callable) -> Response:
        """
        HTTP 요청을 처리하고 상세한 로그를 기록합니다.

        요청별 고유 ID를 생성하여 요청 시작부터 응답까지의 전체 흐름을
        추적하고, 처리 시간과 결과를 로깅합니다.

        Args:
            request (Request): FastAPI 요청 객체
            call_next (Callable): 다음 미들웨어 또는 핸들러

        Returns:
            Response: 처리된 HTTP 응답 (요청 ID와 처리 시간 헤더 포함)

        Raises:
            Exception: 하위 핸들러에서 발생한 예외를 재발생
        """
        # 요청 시작 시간
        start_time = time.time()

        # 요청 ID 생성
        request_id = str(uuid.uuid4())[:8]

        # 요청 로깅
        api_logger.info(
            f"[{request_id}] {request.method} {request.url.path} "
            f"- Client: {request.client.host if request.client else 'Unknown'}"
        )

        try:
            # 요청 처리
            response = await call_next(request)

            # 처리 시간 계산
            process_time = time.time() - start_time

            # 응답 로깅
            api_logger.info(
                f"[{request_id}] Completed in {process_time:.3f}s "
                f"- Status: {response.status_code}"
            )

            # 응답 헤더에 요청 ID 추가
            response.headers["X-Request-ID"] = request_id
            response.headers["X-Process-Time"] = str(process_time)

            return response

        except Exception as e:
            # 에러 로깅
            process_time = time.time() - start_time
            api_logger.error(
                f"[{request_id}] Failed in {process_time:.3f}s "
                f"- Error: {str(e)}"
            )
            raise


class CORSMiddleware(BaseHTTPMiddleware):
    """
    Cross-Origin Resource Sharing (CORS) 처리 미들웨어입니다.

    웹 브라우저의 동일 출처 정책(Same-Origin Policy)을 우회하여
    다른 도메인에서 API에 접근할 수 있도록 CORS 헤더를 설정합니다.

    설정되는 CORS 헤더:
    - Access-Control-Allow-Origin: 모든 도메인에서 접근 허용 (*)
    - Access-Control-Allow-Methods: 허용되는 HTTP 메서드들
    - Access-Control-Allow-Headers: 허용되는 요청 헤더들
    - Access-Control-Max-Age: 프리플라이트 요청 캐시 시간 (24시간)

    지원하는 HTTP 메서드:
    - GET: 데이터 조회
    - POST: 데이터 생성
    - PUT: 데이터 전체 수정
    - DELETE: 데이터 삭제
    - OPTIONS: 프리플라이트 요청

    허용하는 요청 헤더:
    - Content-Type: 요청 본문의 미디어 타입
    - Authorization: 인증 토큰 (JWT 등)
    - X-Requested-With: AJAX 요청 식별

    프론트엔드 애플리케이션에서 API 호출 시 필수적인 미들웨어입니다.
    """

    async def dispatch(self, request: Request, call_next: Callable) -> Response:
        """
        요청을 처리하고 응답에 CORS 헤더를 추가합니다.

        모든 HTTP 응답에 CORS 관련 헤더를 자동으로 추가하여
        브라우저에서 Cross-Origin 요청을 허용합니다.

        Args:
            request (Request): FastAPI 요청 객체
            call_next (Callable): 다음 미들웨어 또는 핸들러

        Returns:
            Response: CORS 헤더가 추가된 HTTP 응답
        """
        response = await call_next(request)

        # CORS 헤더 추가
        response.headers["Access-Control-Allow-Origin"] = "*"
        response.headers["Access-Control-Allow-Methods"] = "GET, POST, PUT, DELETE, OPTIONS"
        response.headers["Access-Control-Allow-Headers"] = "Content-Type, Authorization, X-Requested-With"
        response.headers["Access-Control-Max-Age"] = "86400"

        return response


class SecurityMiddleware(BaseHTTPMiddleware):
    """
    웹 보안 헤더 설정 미들웨어입니다.

    웹 애플리케이션의 보안을 강화하기 위해 다양한 보안 관련 HTTP 헤더를
    자동으로 설정합니다. 이러한 헤더들은 일반적인 웹 보안 취약점을 방지하고
    브라우저의 보안 기능을 활용하여 사용자를 보호합니다.

    설정되는 보안 헤더:
    - X-Content-Type-Options: MIME 타입 스니핑 방지
    - X-Frame-Options: 클릭재킹 공격 방지
    - X-XSS-Protection: XSS 공격 탐지 및 차단
    - Referrer-Policy: 리퍼러 정보 노출 제어

    보안 효과:
    - MIME 스니핑 공격 차단
    - iframe 삽입을 통한 클릭재킹 방지
    - 반사형 XSS 공격 탐지 및 차단
    - 민감한 URL 정보 누출 방지

    이 미들웨어는 OWASP 권장 사항에 따라 기본적인 웹 보안 헤더를
    설정하여 애플리케이션의 보안 수준을 향상시킵니다.
    """

    async def dispatch(self, request: Request, call_next: Callable) -> Response:
        """
        요청을 처리하고 응답에 보안 헤더를 추가합니다.

        모든 HTTP 응답에 웹 보안을 강화하는 헤더들을 자동으로 추가하여
        일반적인 웹 보안 취약점으로부터 사용자를 보호합니다.

        Args:
            request (Request): FastAPI 요청 객체
            call_next (Callable): 다음 미들웨어 또는 핸들러

        Returns:
            Response: 보안 헤더가 추가된 HTTP 응답
        """
        response = await call_next(request)

        # 보안 헤더 추가
        response.headers["X-Content-Type-Options"] = "nosniff"
        response.headers["X-Frame-Options"] = "DENY"
        response.headers["X-XSS-Protection"] = "1; mode=block"
        response.headers["Referrer-Policy"] = "strict-origin-when-cross-origin"

        return response