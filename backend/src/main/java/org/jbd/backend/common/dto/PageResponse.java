package org.jbd.backend.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 페이징된 데이터를 클라이언트에게 전달하기 위한 응답 DTO입니다.
 *
 * Spring Data JPA의 Page 객체를 클라이언트 친화적인 형태로 변환하여
 * 페이징 정보와 실제 데이터를 함께 제공합니다.
 *
 * 포함 정보:
 * - content: 현재 페이지의 실제 데이터 목록
 * - totalElements: 전체 데이터 개수
 * - totalPages: 전체 페이지 수
 * - size: 페이지당 최대 데이터 개수
 * - number: 현재 페이지 번호 (0부터 시작)
 * - first: 첫 번째 페이지 여부
 * - last: 마지막 페이지 여부
 *
 * 사용 예시:
 * - 게시판 목록 조회 결과
 * - 사용자 목록 조회 결과
 * - 검색 결과 목록
 *
 * @param <T> 페이징될 데이터의 타입
 */
public class PageResponse<T> {
    /** 현재 페이지의 실제 데이터 목록 */
    private List<T> content;

    /** 전체 데이터 개수 */
    private long totalElements;

    /** 전체 페이지 수 */
    private int totalPages;

    /** 페이지당 최대 데이터 개수 */
    private int size;

    /** 현재 페이지 번호 (0부터 시작) */
    private int number;

    /** 첫 번째 페이지 여부 */
    private boolean first;

    /** 마지막 페이지 여부 */
    private boolean last;

    /**
     * 기본 생성자입니다.
     * JSON 직렬화/역직렬화를 위해 필요합니다.
     */
    public PageResponse() {}

    /**
     * Spring Data JPA의 Page 객체로부터 PageResponse를 생성합니다.
     *
     * @param page Spring Data JPA Page 객체
     */
    public PageResponse(Page<T> page) {
        this.content = page.getContent();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.size = page.getSize();
        this.number = page.getNumber();
        this.first = page.isFirst();
        this.last = page.isLast();
    }

    /**
     * 모든 페이징 정보를 직접 설정하여 PageResponse를 생성합니다.
     *
     * @param content 현재 페이지의 데이터 목록
     * @param totalElements 전체 데이터 개수
     * @param totalPages 전체 페이지 수
     * @param size 페이지당 최대 데이터 개수
     * @param number 현재 페이지 번호
     * @param first 첫 번째 페이지 여부
     * @param last 마지막 페이지 여부
     */
    public PageResponse(List<T> content, long totalElements, int totalPages, int size, int number, boolean first, boolean last) {
        this.content = content;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.size = size;
        this.number = number;
        this.first = first;
        this.last = last;
    }

    /**
     * 현재 페이지의 데이터 목록을 반환합니다.
     *
     * @return 데이터 목록
     */
    public List<T> getContent() {
        return content;
    }

    /**
     * 현재 페이지의 데이터 목록을 설정합니다.
     *
     * @param content 데이터 목록
     */
    public void setContent(List<T> content) {
        this.content = content;
    }

    /**
     * 전체 데이터 개수를 반환합니다.
     *
     * @return 전체 데이터 개수
     */
    public long getTotalElements() {
        return totalElements;
    }

    /**
     * 전체 데이터 개수를 설정합니다.
     *
     * @param totalElements 전체 데이터 개수
     */
    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    /**
     * 전체 페이지 수를 반환합니다.
     *
     * @return 전체 페이지 수
     */
    public int getTotalPages() {
        return totalPages;
    }

    /**
     * 전체 페이지 수를 설정합니다.
     *
     * @param totalPages 전체 페이지 수
     */
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    /**
     * 페이지당 최대 데이터 개수를 반환합니다.
     *
     * @return 페이지당 최대 데이터 개수
     */
    public int getSize() {
        return size;
    }

    /**
     * 페이지당 최대 데이터 개수를 설정합니다.
     *
     * @param size 페이지당 최대 데이터 개수
     */
    public void setSize(int size) {
        this.size = size;
    }

    /**
     * 현재 페이지 번호를 반환합니다.
     *
     * @return 현재 페이지 번호 (0부터 시작)
     */
    public int getNumber() {
        return number;
    }

    /**
     * 현재 페이지 번호를 설정합니다.
     *
     * @param number 현재 페이지 번호 (0부터 시작)
     */
    public void setNumber(int number) {
        this.number = number;
    }

    /**
     * 첫 번째 페이지인지 확인합니다.
     *
     * @return 첫 번째 페이지인 경우 true
     */
    public boolean isFirst() {
        return first;
    }

    /**
     * 첫 번째 페이지 여부를 설정합니다.
     *
     * @param first 첫 번째 페이지 여부
     */
    public void setFirst(boolean first) {
        this.first = first;
    }

    /**
     * 마지막 페이지인지 확인합니다.
     *
     * @return 마지막 페이지인 경우 true
     */
    public boolean isLast() {
        return last;
    }

    /**
     * 마지막 페이지 여부를 설정합니다.
     *
     * @param last 마지막 페이지 여부
     */
    public void setLast(boolean last) {
        this.last = last;
    }

    /**
     * 현재 페이지에 포함된 실제 데이터 개수를 반환합니다.
     *
     * 페이지 크기(size)와 다를 수 있으며, 마지막 페이지에서는
     * 보통 페이지 크기보다 작은 값을 가집니다.
     *
     * @return 현재 페이지의 실제 데이터 개수
     */
    public int getNumberOfElements() {
        return content != null ? content.size() : 0;
    }
}