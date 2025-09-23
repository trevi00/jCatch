package org.jbd.backend.community.controller;

import jakarta.validation.Valid;
import org.jbd.backend.common.dto.ApiResponse;
import org.jbd.backend.community.dto.CategoryDto;
import org.jbd.backend.community.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@CrossOrigin(origins = "*")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryDto.Response>> createCategory(
            @Valid @RequestBody CategoryDto.Request request) {
        CategoryDto.Response response = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("카테고리가 생성되었습니다.", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryDto.Response>> getCategoryById(
            @PathVariable Long id) {
        CategoryDto.Response response = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success("카테고리 조회 성공", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryDto.Response>>> getAllActiveCategories() {
        List<CategoryDto.Response> responses = categoryService.getAllActiveCategories();
        return ResponseEntity.ok(ApiResponse.success("카테고리 목록 조회 성공", responses));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryDto.Response>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryDto.Request request) {
        CategoryDto.Response response = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success("카테고리가 수정되었습니다.", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("카테고리가 삭제되었습니다."));
    }

    @PatchMapping("/{id}/display-order")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryDto.Response>> updateDisplayOrder(
            @PathVariable Long id,
            @RequestBody Integer displayOrder) {
        CategoryDto.Response response = categoryService.updateDisplayOrder(id, displayOrder);
        return ResponseEntity.ok(ApiResponse.success("카테고리 순서가 변경되었습니다.", response));
    }
}