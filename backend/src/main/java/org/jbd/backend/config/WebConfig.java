package org.jbd.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Spring Web MVC 설정 클래스
 *
 * 웹 애플리케이션의 전반적인 구성을 담당하며, HTTP 메시지 변환, 정적 리소스 처리,
 * 인코딩 설정 등 웹 계층의 핵심 설정을 정의합니다.
 *
 * 주요 기능:
 * - HTTP 메시지 컨버터의 UTF-8 인코딩 설정
 * - 정적 리소스(CSS, JS, 이미지 등) 핸들링
 * - REST API와 정적 리소스 경로 분리
 * - 캐싱 정책 설정
 *
 * @author JBD Backend Team
 * @version 1.0
 * @since 2025-09-19
 * @see WebMvcConfigurer
 * @see HttpMessageConverter
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * HTTP 메시지 컨버터를 구성합니다.
     *
     * 클라이언트와 서버 간의 HTTP 요청/응답 본문을 적절한 객체로 변환하는 컨버터들을
     * UTF-8 인코딩으로 설정하여 한글 등 다국어 처리를 보장합니다.
     *
     * 설정되는 컨버터:
     * - StringHttpMessageConverter: 문자열 데이터 처리 (plain/text)
     * - MappingJackson2HttpMessageConverter: JSON 데이터 처리 (application/json)
     *
     * @param converters Spring에서 제공하는 HTTP 메시지 컨버터 목록
     * @see StringHttpMessageConverter
     * @see MappingJackson2HttpMessageConverter
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // StringHttpMessageConverter를 UTF-8로 설정
        // 텍스트 데이터 전송 시 한글 깨짐 방지
        converters.stream()
            .filter(converter -> converter instanceof StringHttpMessageConverter)
            .forEach(converter -> ((StringHttpMessageConverter) converter)
                .setDefaultCharset(StandardCharsets.UTF_8));

        // MappingJackson2HttpMessageConverter를 UTF-8로 설정
        // JSON 직렬화/역직렬화 시 한글 깨짐 방지
        converters.stream()
            .filter(converter -> converter instanceof MappingJackson2HttpMessageConverter)
            .forEach(converter -> ((MappingJackson2HttpMessageConverter) converter)
                .setDefaultCharset(StandardCharsets.UTF_8));
    }

    /**
     * 정적 리소스 핸들러를 구성합니다.
     *
     * 웹 애플리케이션에서 사용되는 정적 파일들(CSS, JavaScript, 이미지 등)에 대한
     * 요청을 처리하고 적절한 위치에서 파일을 제공합니다.
     *
     * 설정 원칙:
     * - REST API 경로(/api/**)와 충돌하지 않도록 명시적 경로 분리
     * - 파일 타입별로 별도 핸들러 구성하여 관리 용이성 확보
     * - 캐시 정책 적용으로 성능 최적화
     * - 클래스패스 기반 리소스 위치 지정
     *
     * @param registry Spring에서 제공하는 리소스 핸들러 레지스트리
     * @see ResourceHandlerRegistry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 전체 정적 리소스 핸들러 - 기본 정적 파일 접근
        // URL: /static/** -> classpath:/static/
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600); // 1시간 캐시 (성능 최적화)

        // 이미지 전용 핸들러 - 프로필 이미지, 업로드 이미지 등
        // URL: /images/** -> classpath:/static/images/
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/")
                .setCachePeriod(3600);

        // CSS 파일 핸들러 - 스타일시트 파일
        // URL: /css/** -> classpath:/static/css/
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCachePeriod(3600);

        // JavaScript 파일 핸들러 - 클라이언트 사이드 스크립트
        // URL: /js/** -> classpath:/static/js/
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/")
                .setCachePeriod(3600);
    }

}