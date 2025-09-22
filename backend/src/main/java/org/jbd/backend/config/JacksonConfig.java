package org.jbd.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson JSON 라이브러리 설정 클래스
 *
 * Spring Boot에서 JSON 직렬화/역직렬화를 담당하는 Jackson ObjectMapper의
 * 전역 설정을 정의합니다. 주로 Java 8 Time API와의 호환성을 제공합니다.
 *
 * 주요 기능:
 * - Java 8 날짜/시간 API (LocalDate, LocalDateTime 등) 지원
 * - 전역 ObjectMapper 빈 제공
 * - JSON 직렬화/역직렬화 커스터마이징
 *
 * @author JBD Backend Team
 * @version 1.0
 * @since 2025-09-19
 * @see ObjectMapper
 * @see JavaTimeModule
 */
@Configuration
public class JacksonConfig {

    /**
     * 전역 ObjectMapper를 구성합니다.
     *
     * Jackson ObjectMapper는 Java 객체와 JSON 간의 변환을 담당하는 핵심 컴포넌트입니다.
     * JavaTimeModule을 등록하여 Java 8의 LocalDate, LocalDateTime, ZonedDateTime 등
     * 날짜/시간 객체들을 JSON으로 적절히 직렬화할 수 있도록 설정합니다.
     *
     * 설정 효과:
     * - LocalDate, LocalDateTime 등이 ISO-8601 형식으로 직렬화
     * - JSON에서 날짜/시간 문자열을 Java 객체로 역직렬화 지원
     * - REST API 응답에서 일관된 날짜 형식 제공
     *
     * @return ObjectMapper Java Time API를 지원하는 ObjectMapper 인스턴스
     * @see JavaTimeModule
     * @see Primary 어노테이션으로 기본 ObjectMapper로 지정
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Java 8 Time API 모듈 등록
        // LocalDate, LocalDateTime, ZonedDateTime 등의 직렬화/역직렬화 지원
        mapper.registerModule(new JavaTimeModule());

        return mapper;
    }
}