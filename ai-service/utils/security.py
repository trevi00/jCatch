#!/usr/bin/env python3
"""
보안 유틸리티 - XSS 방지, 입력 검증 등
"""

import re
import html
from typing import Any, Dict, List

class SecurityUtils:
    """보안 관련 유틸리티 클래스"""
    
    @staticmethod
    def sanitize_html(text: str) -> str:
        """HTML 태그 제거 및 특수문자 이스케이프"""
        if not isinstance(text, str):
            return str(text)
        
        # HTML 태그 제거
        text = re.sub(r'<[^>]+>', '', text)
        
        # HTML 특수문자 이스케이프
        text = html.escape(text)
        
        # 스크립트 관련 키워드 제거
        dangerous_patterns = [
            r'javascript:', r'vbscript:', r'onload=', r'onerror=', 
            r'onclick=', r'onmouseover=', r'onfocus=', r'onblur=',
            r'onchange=', r'onsubmit=', r'onkeyup=', r'onkeydown='
        ]
        
        for pattern in dangerous_patterns:
            text = re.sub(pattern, '', text, flags=re.IGNORECASE)
        
        return text
    
    @staticmethod
    def validate_user_input(text: str, max_length: int = 5000) -> Dict[str, Any]:
        """사용자 입력 검증"""
        if not isinstance(text, str):
            return {
                "valid": False,
                "error": "텍스트 형식이 아닙니다",
                "sanitized": ""
            }
        
        # 길이 검증
        if len(text) > max_length:
            return {
                "valid": False,
                "error": f"텍스트가 너무 깁니다 (최대 {max_length}자)",
                "sanitized": text[:max_length]
            }
        
        # 빈 텍스트 검증
        if not text.strip():
            return {
                "valid": False,
                "error": "빈 텍스트입니다",
                "sanitized": ""
            }
        
        # HTML 및 스크립트 제거
        sanitized = SecurityUtils.sanitize_html(text)
        
        return {
            "valid": True,
            "error": None,
            "sanitized": sanitized
        }
    
    @staticmethod
    def sanitize_response_data(data: Any) -> Any:
        """응답 데이터 재귀적 sanitize"""
        if isinstance(data, str):
            return SecurityUtils.sanitize_html(data)
        elif isinstance(data, dict):
            return {
                key: SecurityUtils.sanitize_response_data(value) 
                for key, value in data.items()
            }
        elif isinstance(data, list):
            return [
                SecurityUtils.sanitize_response_data(item) 
                for item in data
            ]
        else:
            return data
    
    @staticmethod
    def is_safe_url(url: str) -> bool:
        """URL 안전성 검증"""
        if not isinstance(url, str):
            return False
        
        # 기본적인 URL 형식 검증
        url_pattern = re.compile(
            r'^https?://'  # http:// or https://
            r'(?:(?:[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?\.)+[A-Z]{2,6}\.?|'  # domain...
            r'localhost|'  # localhost...
            r'\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})'  # ...or ip
            r'(?::\d+)?'  # optional port
            r'(?:/?|[/?]\S+)$', re.IGNORECASE)
        
        return url_pattern.match(url) is not None
    
    @staticmethod
    def filter_sql_injection_attempts(text: str) -> str:
        """SQL 인젝션 시도 필터링"""
        if not isinstance(text, str):
            return str(text)
        
        # 위험한 SQL 키워드 패턴
        sql_patterns = [
            r'\bUNION\b', r'\bSELECT\b', r'\bINSERT\b', r'\bUPDATE\b', 
            r'\bDELETE\b', r'\bDROP\b', r'\bCREATE\b', r'\bALTER\b',
            r'\bEXEC\b', r'\bEXECUTE\b', r'--', r'/\*', r'\*/',
            r'\bSCRIPT\b', r'<script', r'</script>'
        ]
        
        for pattern in sql_patterns:
            text = re.sub(pattern, '', text, flags=re.IGNORECASE)
        
        return text.strip()

# 전역 인스턴스
security_utils = SecurityUtils()