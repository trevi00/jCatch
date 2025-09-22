#!/usr/bin/env python3
"""
번역 서비스 - 문서 및 텍스트 다국어 번역
"""

import logging
from typing import List, Dict, Any, Optional
import json
from datetime import datetime

from services.openai_service import openai_service
from config import settings

logger = logging.getLogger(__name__)

class TranslationService:
    """번역 서비스 클래스"""
    
    def __init__(self):
        """서비스 초기화"""
        self.client = openai_service.client
        self.supported_languages = {
            "ko": "한국어",
            "en": "영어", 
            "ja": "일본어",
            "zh": "중국어",
            "fr": "프랑스어",
            "de": "독일어",
            "es": "스페인어",
            "it": "이탈리아어",
            "pt": "포르투갈어",
            "ru": "러시아어"
        }
        
        self.document_templates = {
            "resume": {
                "name": "이력서",
                "context": "전문적이고 간결한 이력서 번역. 기술 용어는 정확하게, 경력사항은 명확하게 번역.",
                "style": "formal, professional"
            },
            "cover_letter": {
                "name": "자기소개서",
                "context": "지원자의 열정과 역량이 잘 전달되도록 번역. 개인적 경험과 동기를 자연스럽게 표현.",
                "style": "professional, persuasive"
            },
            "portfolio": {
                "name": "포트폴리오",
                "context": "프로젝트와 성과를 효과적으로 어필할 수 있도록 번역. 기술적 내용의 정확성 중시.",
                "style": "technical, clear"
            },
            "email": {
                "name": "이메일",
                "context": "비즈니스 이메일의 예의와 목적이 명확히 전달되도록 번역.",
                "style": "polite, direct"
            }
        }
    
    async def translate(
        self,
        text: str,
        target_language: str,
        source_language: str = "auto",
        document_type: Optional[str] = None
    ) -> Dict[str, Any]:
        """기본 텍스트 번역"""
        try:
            logger.info(f"번역 요청 - {source_language} -> {target_language}")
            
            if target_language not in self.supported_languages:
                return {
                    "success": False,
                    "error": f"지원하지 않는 언어입니다: {target_language}"
                }
            
            prompt = self._create_translation_prompt(
                text, source_language, target_language, document_type
            )
            
            response = self.client.chat.completions.create(
                model=settings.LLM_MODEL,
                messages=[{"role": "user", "content": prompt}],
                temperature=0.3,
                max_tokens=2000
            )
            
            translated_text = response.choices[0].message.content.strip()
            
            logger.info("번역 완료")
            
            return {
                "success": True,
                "original_text": text,
                "translated_text": translated_text,
                "source_language": source_language,
                "target_language": target_language,
                "target_language_name": self.supported_languages[target_language],
                "document_type": document_type,
                "translated_at": datetime.now().isoformat()
            }
            
        except Exception as e:
            logger.error(f"번역 실패: {e}")
            return {
                "success": False,
                "error": str(e)
            }
    
    async def batch_translate(
        self,
        texts: List[str],
        target_language: str,
        source_language: str = "auto"
    ) -> Dict[str, Any]:
        """일괄 번역"""
        try:
            logger.info(f"일괄 번역 요청 - {len(texts)}개 텍스트")
            
            translations = []
            for i, text in enumerate(texts):
                result = await self.translate(text, target_language, source_language)
                if result["success"]:
                    translations.append({
                        "index": i,
                        "original": text,
                        "translated": result["translated_text"]
                    })
                else:
                    translations.append({
                        "index": i,
                        "original": text,
                        "translated": text,  # 실패시 원문 유지
                        "error": result.get("error")
                    })
            
            success_count = len([t for t in translations if "error" not in t])
            
            return {
                "success": True,
                "total_texts": len(texts),
                "successful_translations": success_count,
                "failed_translations": len(texts) - success_count,
                "translations": translations,
                "target_language": target_language,
                "translated_at": datetime.now().isoformat()
            }
            
        except Exception as e:
            logger.error(f"일괄 번역 실패: {e}")
            return {
                "success": False,
                "error": str(e)
            }
    
    def get_supported_languages(self) -> Dict[str, Any]:
        """지원 언어 목록 조회"""
        return {
            "success": True,
            "languages": self.supported_languages
        }
    
    def get_templates(self) -> Dict[str, Any]:
        """문서 템플릿 목록 조회"""
        return {
            "success": True,
            "templates": self.document_templates
        }
    
    async def translate_document(
        self,
        text: str,
        document_type: str,
        target_language: str,
        source_language: str = "auto"
    ) -> Dict[str, Any]:
        """문서 유형별 맞춤 번역"""
        try:
            if document_type not in self.document_templates:
                return {
                    "success": False,
                    "error": f"지원하지 않는 문서 유형: {document_type}"
                }
            
            result = await self.translate(
                text, target_language, source_language, document_type
            )
            
            if result["success"]:
                # 문서 유형별 후처리
                result["document_info"] = self.document_templates[document_type]
                
            return result
            
        except Exception as e:
            logger.error(f"문서 번역 실패: {e}")
            return {
                "success": False,
                "error": str(e)
            }
    
    def _create_translation_prompt(
        self,
        text: str,
        source_language: str,
        target_language: str,
        document_type: Optional[str] = None
    ) -> str:
        """번역 프롬프트 생성"""
        
        target_lang_name = self.supported_languages.get(target_language, target_language)
        
        base_prompt = f"""
다음 텍스트를 {target_lang_name}로 번역해주세요.

원문:
{text}

번역 요구사항:
1. 정확하고 자연스러운 번역
2. 원문의 의미와 뉘앙스 보존
3. 목적어의 문화적 맥락 고려
4. 전문 용어는 해당 분야의 표준 용어 사용
"""

        if document_type and document_type in self.document_templates:
            template_info = self.document_templates[document_type]
            base_prompt += f"""

문서 유형: {template_info['name']}
번역 맥락: {template_info['context']}
번역 스타일: {template_info['style']}
"""

        base_prompt += "\n번역 결과만 출력해주세요:"
        
        return base_prompt
    
    def _detect_language(self, text: str) -> str:
        """언어 감지 (간단한 휴리스틱)"""
        # 한글 포함 여부 확인
        if any('\uac00' <= char <= '\ud7af' for char in text):
            return "ko"
        
        # 일본어 확인 (히라가나, 가타카나)
        if any('\u3040' <= char <= '\u309f' or '\u30a0' <= char <= '\u30ff' for char in text):
            return "ja"
        
        # 중국어 확인 (한자)
        if any('\u4e00' <= char <= '\u9fff' for char in text):
            return "zh"
        
        # 기본값은 영어
        return "en"
    
    def get_translation_quality_score(self, original: str, translated: str) -> Dict[str, Any]:
        """번역 품질 점수 (간단한 메트릭)"""
        try:
            # 길이 비율
            length_ratio = len(translated) / len(original) if original else 0
            
            # 기본 점수 계산
            if 0.5 <= length_ratio <= 2.0:
                length_score = 100
            elif 0.3 <= length_ratio <= 3.0:
                length_score = 80
            else:
                length_score = 60
            
            # 특수문자/숫자 보존도
            original_special = len([c for c in original if not c.isalnum() and not c.isspace()])
            translated_special = len([c for c in translated if not c.isalnum() and not c.isspace()])
            special_preservation = min(1.0, translated_special / max(1, original_special)) * 100
            
            overall_score = (length_score + special_preservation) / 2
            
            return {
                "overall_score": round(overall_score, 1),
                "length_score": length_score,
                "special_preservation": round(special_preservation, 1),
                "length_ratio": round(length_ratio, 2)
            }
            
        except Exception as e:
            logger.error(f"품질 점수 계산 실패: {e}")
            return {
                "overall_score": 75.0,
                "length_score": 75.0,
                "special_preservation": 75.0,
                "length_ratio": 1.0
            }

# 전역 인스턴스
translation_service = TranslationService()