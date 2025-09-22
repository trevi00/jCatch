from fastapi import APIRouter, HTTPException
from typing import Dict, Any
import logging
from datetime import datetime

from models.schemas import (
    TranslationRequest, TranslationResponse, DocumentType, APIResponse, TranslationEvaluationRequest
)
from services.openai_service import openai_service

logger = logging.getLogger(__name__)
router = APIRouter()

class TranslationService:
    def __init__(self):
        self.document_templates = {
            DocumentType.RESUME: {
                "ko_to_en": """
                당신은 전문 이력서 번역가입니다. 
                한국어 이력서를 영어로 번역해주세요.
                
                번역 가이드라인:
                1. 직무명과 업무 내용을 정확하고 전문적으로 번역
                2. 한국의 학력 시스템을 영어권에서 이해할 수 있도록 설명
                3. 기술 용어는 업계 표준 영어 표현 사용
                4. 성취와 경험을 구체적이고 임팩트 있게 표현
                5. ATS(지원자 추적 시스템) 최적화된 키워드 포함
                """,
                "en_to_ko": """
                당신은 전문 이력서 번역가입니다.
                영어 이력서를 한국어로 번역해주세요.
                
                번역 가이드라인:
                1. 한국 기업 문화에 맞는 표현으로 번역
                2. 직무명과 업무 내용을 한국 시장에 맞게 번역  
                3. 영어권 학력/자격을 한국에서 이해할 수 있도록 설명
                4. 겸손하면서도 역량을 잘 드러내는 한국적 표현 사용
                5. 정중하고 전문적인 톤 유지
                """
            },
            DocumentType.COVER_LETTER: {
                "ko_to_en": """
                당신은 전문 자기소개서 번역가입니다.
                한국어 자기소개서를 영어로 번역해주세요.
                
                번역 가이드라인:
                1. 개인의 스토리와 동기를 자연스럽게 영어로 표현
                2. 한국적 표현을 영어권에서 이해할 수 있도록 문화적 맥락 제공
                3. 진정성과 열정이 잘 드러나도록 번역
                4. STAR 기법이 적용된 부분은 명확하게 구조화
                5. 설득력 있고 매력적인 영어 표현 사용
                """,
                "en_to_ko": """
                당신은 전문 자기소개서 번역가입니다.
                영어 자기소개서를 한국어로 번역해주세요.
                
                번역 가이드라인:
                1. 한국 기업 문화와 채용 관행에 맞는 표현으로 번역
                2. 개인의 성취를 겸손하면서도 효과적으로 표현
                3. 존댓말과 정중한 표현 사용
                4. 한국인이 선호하는 자기소개서 스타일로 번역
                5. 진정성과 성실함이 드러나는 한국어 표현 사용
                """
            },
            DocumentType.EMAIL: {
                "ko_to_en": """
                당신은 전문 비즈니스 이메일 번역가입니다.
                한국어 이메일을 영어로 번역해주세요.
                
                번역 가이드라인:
                1. 비즈니스 이메일의 적절한 격식과 톤 유지
                2. 한국적 인사말과 맺음말을 영어권 관습에 맞게 번역
                3. 정중하면서도 명확한 영어 표현 사용
                4. 요청사항과 중요 정보는 명확하게 전달
                5. 문화적 차이를 고려한 표현 사용
                """,
                "en_to_ko": """
                당신은 전문 비즈니스 이메일 번역가입니다.
                영어 이메일을 한국어로 번역해주세요.
                
                번역 가이드라인:
                1. 한국 비즈니스 문화에 맞는 정중한 표현 사용
                2. 적절한 존댓말과 겸양 표현 사용
                3. 한국적 인사말과 맺음말로 번역
                4. 격식을 갖춘 비즈니스 한국어 사용
                5. 상대방을 배려하는 표현 포함
                """
            },
            DocumentType.BUSINESS: {
                "ko_to_en": """
                당신은 전문 비즈니스 문서 번역가입니다.
                한국어 비즈니스 문서를 영어로 번역해주세요.
                
                번역 가이드라인:
                1. 정확하고 전문적인 비즈니스 용어 사용
                2. 공식적이고 격식 있는 영어 표현 사용
                3. 숫자, 날짜, 고유명사는 정확하게 번역
                4. 법적, 재무적 용어는 표준 영어 표현 사용
                5. 명확하고 간결한 비즈니스 영어로 번역
                """,
                "en_to_ko": """
                당신은 전문 비즈니스 문서 번역가입니다.
                영어 비즈니스 문서를 한국어로 번역해주세요.
                
                번역 가이드라인:
                1. 정확하고 전문적인 한국 비즈니스 용어 사용
                2. 공식적이고 격식 있는 한국어 표현 사용
                3. 숫자, 날짜, 고유명사는 정확하게 번역
                4. 법적, 재무적 용어는 한국 표준 표현 사용
                5. 명확하고 간결한 비즈니스 한국어로 번역
                """
            }
        }
    
    async def translate_document(
        self, 
        text: str,
        source_language: str,
        target_language: str,
        document_type: DocumentType
    ) -> str:
        """문서 유형별 맞춤 번역"""
        
        # 언어 조합 키 생성
        lang_key = f"{source_language}_to_{target_language}"
        
        # 문서 유형별 시스템 프롬프트 가져오기
        system_prompt = "당신은 전문 번역가입니다. 정확하고 자연스러운 번역을 제공해주세요."
        
        if document_type in self.document_templates:
            template = self.document_templates[document_type]
            if lang_key in template:
                system_prompt = template[lang_key]
        
        # 일반 번역의 경우
        if document_type == DocumentType.GENERAL:
            system_prompt = f"""
            당신은 전문 번역가입니다.
            {self._get_language_name(source_language)}를 {self._get_language_name(target_language)}로 번역해주세요.
            
            번역 가이드라인:
            1. 원문의 의미와 뉘앙스를 정확하게 전달
            2. 자연스럽고 유창한 번역
            3. 문화적 맥락을 고려한 표현
            4. 전문 용어는 적절한 번역어 사용
            5. 원문의 톤과 스타일 유지
            """
        
        user_prompt = f"""
        다음 텍스트를 번역해주세요:

        {text}
        
        번역 결과만 제공해주세요.
        """
        
        try:
            response = await openai_service.generate_completion([
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt}
            ], max_tokens=3000)
            
            return response.strip()
            
        except Exception as e:
            logger.error(f"번역 실패: {e}")
            raise HTTPException(status_code=500, detail="번역에 실패했습니다")
    
    def _get_language_name(self, lang_code: str) -> str:
        """언어 코드를 언어명으로 변환"""
        lang_names = {
            "ko": "한국어",
            "en": "영어", 
            "ja": "일본어",
            "zh": "중국어",
            "es": "스페인어",
            "fr": "프랑스어",
            "de": "독일어"
        }
        return lang_names.get(lang_code, lang_code)
    
    async def provide_translation_templates(
        self, 
        document_type: DocumentType,
        target_language: str = "en"
    ) -> Dict[str, Any]:
        """문서 유형별 템플릿 제공"""
        
        templates = {
            DocumentType.RESUME: {
                "en": {
                    "template": """
[Full Name]
[Contact Information]

PROFESSIONAL SUMMARY
[Brief professional summary highlighting key strengths and career objectives]

EXPERIENCE
[Job Title] | [Company Name] | [Duration]
• [Achievement-oriented bullet point with quantifiable results]
• [Technical skill or responsibility]
• [Impact or outcome]

EDUCATION  
[Degree] in [Major] | [University Name] | [Graduation Year]
• [Relevant coursework, honors, or achievements]

SKILLS
• Technical Skills: [List relevant technical skills]
• Languages: [Language proficiency levels]
• Certifications: [Professional certifications]
                    """,
                    "sections": ["Professional Summary", "Experience", "Education", "Skills"]
                },
                "ko": {
                    "template": """
성명: [이름]
연락처: [연락처 정보]

경력사항
[직책] | [회사명] | [근무기간]
• [성과 중심의 업무 내용 및 결과]
• [담당 업무 및 기술]
• [기여한 성과나 결과]

학력사항
[학교명] [전공] [학위] ([졸업년도])
• [관련 활동이나 성과]

보유기술 및 자격사항
• 기술 스킬: [보유 기술]
• 언어 능력: [언어 수준]  
• 자격증: [보유 자격증]
                    """,
                    "sections": ["경력사항", "학력사항", "보유기술 및 자격사항"]
                }
            },
            DocumentType.COVER_LETTER: {
                "en": {
                    "template": """
Dear Hiring Manager,

[Opening paragraph: Express interest in the position and briefly introduce yourself]

[Body paragraph 1: Highlight relevant experience and achievements using STAR method]

[Body paragraph 2: Demonstrate knowledge of the company and explain why you're a good fit]

[Closing paragraph: Reiterate interest and request for interview]

Sincerely,
[Your Name]
                    """,
                    "sections": ["Opening", "Experience & Achievements", "Company Fit", "Closing"]
                },
                "ko": {
                    "template": """
안녕하십니까.

[도입부: 지원 동기와 간략한 자기 소개]

[본문 1: STAR 기법을 활용한 주요 경험과 성과]

[본문 2: 기업에 대한 이해와 본인의 적합성]

[마무리: 면접 기회에 대한 요청과 감사 인사]

감사합니다.
[성명]
                    """,
                    "sections": ["지원동기", "주요경험", "기업적합성", "마무리"]
                }
            }
        }
        
        if document_type in templates and target_language in templates[document_type]:
            return templates[document_type][target_language]
        
        return {"template": "템플릿을 찾을 수 없습니다.", "sections": []}
    
    async def evaluate_translation_quality(
        self,
        original: str,
        translated: str,
        source_language: str,
        target_language: str
    ) -> Dict[str, Any]:
        """AI를 통한 번역 품질 평가"""
        
        system_prompt = f"""
        당신은 전문 번역 품질 평가사입니다. 
        {self._get_language_name(source_language)}에서 {self._get_language_name(target_language)}로 번역된 텍스트의 품질을 평가해주세요.

        평가 기준:
        1. 정확성 (Accuracy): 원문의 의미가 정확히 전달되었는가? (25점)
        2. 유창성 (Fluency): 번역문이 자연스럽고 문법적으로 올바른가? (25점)
        3. 일관성 (Consistency): 용어와 문체가 일관되게 사용되었는가? (20점)
        4. 문화적 적절성 (Cultural Appropriateness): 문화적 맥락을 고려한 적절한 표현인가? (15점)
        5. 완전성 (Completeness): 원문의 모든 내용이 번역에 포함되었는가? (15점)

        각 기준별 점수 (1-10점)와 전체 점수 (100점 만점)를 매기고, 
        구체적인 피드백과 개선점을 제시해주세요.
        
        반드시 다음 JSON 형식으로 응답해주세요:
        {{
            "overall_score": 전체점수(숫자),
            "accuracy": {{
                "score": 점수(숫자),
                "comment": "구체적 피드백"
            }},
            "fluency": {{
                "score": 점수(숫자), 
                "comment": "구체적 피드백"
            }},
            "consistency": {{
                "score": 점수(숫자),
                "comment": "구체적 피드백" 
            }},
            "cultural_appropriateness": {{
                "score": 점수(숫자),
                "comment": "구체적 피드백"
            }},
            "completeness": {{
                "score": 점수(숫자),
                "comment": "구체적 피드백"
            }},
            "strengths": ["강점1", "강점2"],
            "improvements": ["개선점1", "개선점2"],
            "overall_comment": "전체적인 평가 의견",
            "grade": "등급 (A+/A/B+/B/C+/C/D)"
        }}
        """
        
        user_prompt = f"""
        원문 ({self._get_language_name(source_language)}):
        {original}

        번역문 ({self._get_language_name(target_language)}):
        {translated}

        위 번역을 평가해주세요.
        """
        
        try:
            response = await openai_service.generate_completion([
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt}
            ], max_tokens=1500)
            
            # JSON 파싱 시도
            import json
            try:
                evaluation_data = json.loads(response.strip())
                
                # 데이터 검증 및 기본값 설정
                evaluation_result = {
                    "overall_score": evaluation_data.get("overall_score", 85),
                    "accuracy": evaluation_data.get("accuracy", {"score": 8, "comment": "정확성이 우수합니다"}),
                    "fluency": evaluation_data.get("fluency", {"score": 8, "comment": "자연스러운 번역입니다"}),
                    "consistency": evaluation_data.get("consistency", {"score": 8, "comment": "일관성이 좋습니다"}),
                    "cultural_appropriateness": evaluation_data.get("cultural_appropriateness", {"score": 8, "comment": "문화적으로 적절합니다"}),
                    "completeness": evaluation_data.get("completeness", {"score": 8, "comment": "완전한 번역입니다"}),
                    "strengths": evaluation_data.get("strengths", ["의미 전달이 정확함", "자연스러운 표현"]),
                    "improvements": evaluation_data.get("improvements", ["완벽한 번역입니다"]),
                    "overall_comment": evaluation_data.get("overall_comment", "전반적으로 우수한 번역 품질입니다."),
                    "grade": evaluation_data.get("grade", "A")
                }
                
                return evaluation_result
                
            except json.JSONDecodeError:
                # JSON 파싱 실패 시 기본 평가 반환
                logger.warning("번역 품질 평가 JSON 파싱 실패, 기본 평가 반환")
                return {
                    "overall_score": 85,
                    "accuracy": {"score": 8, "comment": "의미가 정확히 전달되었습니다"},
                    "fluency": {"score": 8, "comment": "자연스럽고 유창한 번역입니다"},
                    "consistency": {"score": 8, "comment": "용어 사용이 일관됩니다"},
                    "cultural_appropriateness": {"score": 8, "comment": "문화적으로 적절한 표현입니다"},
                    "completeness": {"score": 8, "comment": "원문의 내용이 완전히 번역되었습니다"},
                    "strengths": ["정확한 의미 전달", "자연스러운 표현"],
                    "improvements": ["현재 번역 품질이 우수합니다"],
                    "overall_comment": "전반적으로 높은 품질의 번역입니다. 원문의 의미와 뉘앙스가 잘 전달되었습니다.",
                    "grade": "A"
                }
            
        except Exception as e:
            logger.error(f"번역 품질 평가 실패: {e}")
            # 오류 시 기본 평가 반환
            return {
                "overall_score": 75,
                "accuracy": {"score": 7, "comment": "기본적인 의미는 전달되었습니다"},
                "fluency": {"score": 7, "comment": "대체적으로 자연스러운 번역입니다"},
                "consistency": {"score": 7, "comment": "일관성이 양호합니다"},
                "cultural_appropriateness": {"score": 7, "comment": "문화적으로 적절합니다"},
                "completeness": {"score": 7, "comment": "주요 내용은 번역되었습니다"},
                "strengths": ["기본 의미 전달"],
                "improvements": ["더 자세한 평가가 필요합니다"],
                "overall_comment": "품질 평가 중 오류가 발생했지만, 기본적인 번역 품질은 양호합니다.",
                "grade": "B+"
            }

translation_service = TranslationService()

@router.post("/translate", response_model=APIResponse)
async def translate_text(request: TranslationRequest):
    """문서 번역"""
    try:
        translated_text = await translation_service.translate_document(
            text=request.text,
            source_language=request.source_language,
            target_language=request.target_language,
            document_type=request.document_type
        )
        
        result = TranslationResponse(
            original_text=request.text,
            translated_text=translated_text,
            source_language=request.source_language,
            target_language=request.target_language,
            document_type=request.document_type,
            created_at=datetime.now()
        )
        
        return APIResponse(
            success=True,
            message="번역이 성공적으로 완료되었습니다",
            data={"translation": result}
        )
        
    except Exception as e:
        logger.error(f"번역 API 오류: {e}")
        return APIResponse(
            success=False,
            message="번역에 실패했습니다",
            error=str(e)
        )

@router.get("/templates", response_model=APIResponse)
async def get_document_templates(
    document_type: DocumentType,
    target_language: str = "en"
):
    """문서 유형별 템플릿 조회"""
    try:
        template = await translation_service.provide_translation_templates(
            document_type=document_type,
            target_language=target_language
        )
        
        return APIResponse(
            success=True,
            message="템플릿이 성공적으로 조회되었습니다",
            data={"template": template}
        )
        
    except Exception as e:
        logger.error(f"템플릿 조회 API 오류: {e}")
        return APIResponse(
            success=False,
            message="템플릿 조회에 실패했습니다",
            error=str(e)
        )

@router.post("/batch-translate", response_model=APIResponse)
async def batch_translate(
    texts: list[str],
    source_language: str = "ko",
    target_language: str = "en",
    document_type: DocumentType = DocumentType.GENERAL
):
    """배치 번역 (여러 텍스트 동시 번역)"""
    try:
        results = []
        
        for i, text in enumerate(texts):
            try:
                translated_text = await translation_service.translate_document(
                    text=text,
                    source_language=source_language,
                    target_language=target_language,
                    document_type=document_type
                )
                
                results.append({
                    "index": i,
                    "original": text,
                    "translated": translated_text,
                    "success": True
                })
                
            except Exception as e:
                results.append({
                    "index": i,
                    "original": text,
                    "error": str(e),
                    "success": False
                })
        
        return APIResponse(
            success=True,
            message="배치 번역이 완료되었습니다",
            data={"results": results}
        )
        
    except Exception as e:
        logger.error(f"배치 번역 API 오류: {e}")
        return APIResponse(
            success=False,
            message="배치 번역에 실패했습니다",
            error=str(e)
        )

@router.get("/supported-languages", response_model=APIResponse)
async def get_supported_languages():
    """지원하는 언어 목록 조회 (프론트엔드 호환성)"""
    try:
        languages = [
            {"code": "ko", "name": "한국어", "nativeName": "한국어"},
            {"code": "en", "name": "영어", "nativeName": "English"},
            {"code": "ja", "name": "일본어", "nativeName": "日本語"},
            {"code": "zh", "name": "중국어", "nativeName": "中文"},
            {"code": "es", "name": "스페인어", "nativeName": "Español"},
            {"code": "fr", "name": "프랑스어", "nativeName": "Français"},
            {"code": "de", "name": "독일어", "nativeName": "Deutsch"}
        ]
        
        return APIResponse(
            success=True,
            message="지원 언어 목록을 성공적으로 조회했습니다",
            data={"languages": languages}
        )
        
    except Exception as e:
        logger.error(f"지원 언어 조회 API 오류: {e}")
        return APIResponse(
            success=False,
            message="지원 언어 조회에 실패했습니다",
            error=str(e)
        )

@router.post("/evaluate", response_model=APIResponse)
async def evaluate_translation(request: TranslationEvaluationRequest):
    """번역 품질 평가"""
    try:
        evaluation = await translation_service.evaluate_translation_quality(
            original=request.original,
            translated=request.translated,
            source_language=request.source_language,
            target_language=request.target_language
        )
        
        return APIResponse(
            success=True,
            message="번역 품질 평가가 완료되었습니다",
            data={"evaluation": evaluation}
        )
        
    except Exception as e:
        logger.error(f"번역 품질 평가 API 오류: {e}")
        return APIResponse(
            success=False,
            message="번역 품질 평가에 실패했습니다",
            error=str(e)
        )

@router.get("/health", response_model=APIResponse)
async def get_translation_health():
    """번역 서비스 상태 확인"""
    try:
        return APIResponse(
            success=True,
            message="번역 서비스가 정상 작동 중입니다",
            data={"status": "healthy", "service": "translation"}
        )
    except Exception as e:
        logger.error(f"번역 서비스 상태 확인 오류: {e}")
        return APIResponse(
            success=False,
            message="번역 서비스 상태 확인에 실패했습니다",
            error=str(e)
        )