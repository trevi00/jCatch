package org.jbd.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "JBD API Documentation",
        description = """
            JBD (잡았다) - 종합 취업 지원 솔루션 API

            ## 주요 기능
            - 🤖 **AI 면접 시뮬레이션**: OpenAI 기반 맞춤형 면접 연습
            - 📝 **자기소개서 생성**: AI 지원 자기소개서 작성 도구
            - 🌍 **번역 서비스**: 이력서/자기소개서 다국어 번역
            - 💼 **채용 공고 관리**: 기업용 채용 공고 등록 및 관리
            - 👤 **프로필 관리**: 상세한 사용자 프로필 및 경력 관리
            - 💬 **커뮤니티**: 취업 정보 공유 게시판
            - 📧 **웹메일**: Gmail 연동 이메일 발송 서비스
            - 🎯 **대시보드**: 개인/기업별 통계 및 분석

            ## 인증 방식
            - JWT Bearer Token 기반 인증
            - Google OAuth2 소셜 로그인 지원

            ## 사용자 타입
            - **GENERAL**: 일반 구직자
            - **COMPANY**: 기업 채용담당자
            - **ADMIN**: 시스템 관리자
            """,
        version = "v1.0.0",
        contact = @Contact(
            name = "JBD Development Team",
            email = "dev@jbd.com",
            url = "https://jbd.com"
        ),
        license = @License(
            name = "MIT License",
            url = "https://opensource.org/licenses/MIT"
        )
    ),
    servers = {
        @Server(
            description = "Local Development Server",
            url = "http://localhost:8081"
        ),
        @Server(
            description = "Production Server",
            url = "https://api.jbd.com"
        )
    },
    security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    scheme = "bearer",
    description = "JWT Bearer Token을 사용한 인증입니다. 로그인 후 받은 JWT 토큰을 입력하세요."
)
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new io.swagger.v3.oas.models.security.SecurityScheme()
                                        .type(Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT Bearer Token")
                        )
                );
    }
}