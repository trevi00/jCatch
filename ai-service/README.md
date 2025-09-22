# 잡았다 AI Service

국비 학원 원생 취업 지원 솔루션의 AI 서비스 모듈입니다.

## 주요 기능

### 1. 🎯 AI 면접 시스템
- **기술면접**: 포지션별 맞춤 기술 질문 생성 및 답변 평가
- **인성면접**: STAR 기법 기반 인성 질문 생성 및 피드백
- **실시간 평가**: 답변에 대한 즉시 점수 및 개선사항 제공
- **종합 분석**: 전체 면접에 대한 종합 평가 및 리포트

### 2. 📝 RAG 기반 자소서 생성
- **단계별 질문**: 섹션별 인터랙티브 질문 생성
- **맞춤형 생성**: 기업/직무에 특화된 자소서 작성
- **RAG 시스템**: 자소서 데이터셋 기반 고품질 콘텐츠 생성
- **피드백 제공**: AI 기반 자소서 평가 및 개선 제안

### 3. 🌏 문서 번역
- **전문 번역**: 이력서, 자소서, 비즈니스 문서 전문 번역
- **템플릿 제공**: 문서 유형별 번역 템플릿
- **배치 번역**: 여러 문서 동시 번역
- **문화적 맥락**: 한국/영어권 문화 특성을 고려한 번역

### 4. 🎨 AI 이미지 생성
- **감정 기반 생성**: 텍스트 감정 분석을 통한 맞춤 이미지
- **다양한 스타일**: realistic, artistic, cartoon 등 다양한 스타일
- **변형 생성**: 동일 프롬프트로 여러 변형 이미지
- **커뮤니티 연동**: 게시글 감정에 맞는 이미지 자동 생성

## 기술 스택

- **Framework**: FastAPI 0.104.1
- **AI**: OpenAI GPT-3.5-turbo, DALL-E-3
- **RAG**: LangChain, ChromaDB, sentence-transformers
- **Vector Search**: FAISS
- **HTTP**: uvicorn, requests

## 설치 및 실행

### 1. 의존성 설치
```bash
pip install -r requirements.txt
```

### 2. 환경 변수 설정
```bash
# .env 파일 생성
cp .env.example .env

# OpenAI API 키 설정
OPENAI_API_KEY=your_openai_api_key_here
```

### 3. 서비스 실행
```bash
# 개발 모드 실행
python run.py

# 또는 직접 실행
uvicorn main:app --host 0.0.0.0 --port 8001 --reload
```

### 4. API 문서 확인
서비스 실행 후 다음 URL에서 API 문서를 확인할 수 있습니다:
- Swagger UI: http://localhost:8001/docs
- ReDoc: http://localhost:8001/redoc

## API 엔드포인트

### 면접 관련
- `POST /api/v1/interview/generate-questions`: 면접 질문 생성
- `POST /api/v1/interview/evaluate-answer`: 답변 평가
- `POST /api/v1/interview/complete-interview`: 면접 완료 처리

### 자소서 관련
- `POST /api/v1/cover-letter/generate-questions`: 인터랙티브 질문 생성
- `POST /api/v1/cover-letter/generate-section`: 섹션별 자소서 생성
- `POST /api/v1/cover-letter/generate-complete`: 완전한 자소서 생성
- `POST /api/v1/cover-letter/feedback`: 자소서 피드백

### 번역 관련
- `POST /api/v1/translation/translate`: 문서 번역
- `GET /api/v1/translation/templates`: 문서 템플릿 조회
- `POST /api/v1/translation/batch-translate`: 배치 번역

### 이미지 생성 관련
- `POST /api/v1/image/generate`: 기본 이미지 생성
- `POST /api/v1/image/generate-with-sentiment`: 감정 기반 이미지 생성
- `POST /api/v1/image/generate-variations`: 변형 이미지 생성
- `POST /api/v1/image/analyze-sentiment`: 텍스트 감정 분석

## 테스트

### 통합 테스트 실행
```bash
python test_integration.py
```

### Spring Boot 연동 테스트
Spring Boot 서버(포트 8080)와 FastAPI 서버(포트 8001)를 모두 실행한 후:
```bash
curl -X GET "http://localhost:8080/api/v1/ai/health"
```

## 프로젝트 구조

```
ai-service/
├── config/                 # 설정 파일들
│   └── settings.py
├── models/                 # 데이터 모델
│   └── schemas.py
├── routers/                # API 라우터
│   ├── interview.py
│   ├── cover_letter.py
│   ├── translation.py
│   └── image_generation.py
├── services/               # 비즈니스 로직
│   ├── openai_service.py
│   └── rag_service.py
├── resume_dataset/         # 자소서 RAG 데이터셋
├── data/                   # 벡터 저장소 (자동 생성)
├── main.py                 # FastAPI 앱
├── run.py                  # 실행 스크립트
├── requirements.txt        # 의존성
└── test_integration.py     # 통합 테스트
```

## RAG 시스템

자소서 생성에 사용되는 RAG 시스템은 다음과 같이 구성됩니다:

1. **데이터셋**: `resume_dataset/` 폴더의 텍스트 파일들
2. **벡터화**: sentence-transformers 모델로 임베딩 생성
3. **저장소**: FAISS 인덱스로 벡터 검색
4. **검색**: 사용자 질의에 대한 관련 컨텍스트 검색
5. **생성**: OpenAI GPT 모델로 컨텍스트 기반 자소서 생성

## 개발 가이드

### 새로운 API 추가
1. `models/schemas.py`에 요청/응답 모델 추가
2. `routers/` 폴더에 라우터 파일 생성
3. `services/` 폴더에 비즈니스 로직 구현
4. `main.py`에 라우터 등록

### 환경별 설정
- 개발: `reload=True`, 자세한 로깅
- 운영: `reload=False`, 성능 최적화

## 주의사항

1. **API 비용**: OpenAI API 사용량에 따른 비용 발생
2. **토큰 제한**: 모델별 최대 토큰 수 제한 고려
3. **속도**: 복잡한 요청은 응답 시간이 길 수 있음
4. **에러 처리**: 외부 API 의존성으로 인한 에러 처리 필요

## 모니터링

- **헬스 체크**: `/health` 엔드포인트
- **로그**: 각 API 호출 및 에러 로그
- **메트릭**: 응답 시간, 성공/실패율 추적

## 문의

기술적 문의사항이나 버그 리포트는 프로젝트 이슈 트래커를 통해 제출해주세요.