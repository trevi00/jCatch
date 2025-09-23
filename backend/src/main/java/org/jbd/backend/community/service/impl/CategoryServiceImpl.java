package org.jbd.backend.community.service.impl;

import org.jbd.backend.common.exception.ResourceNotFoundException;
import org.jbd.backend.community.domain.Category;
import org.jbd.backend.community.dto.CategoryDto;
import org.jbd.backend.community.repository.CategoryRepository;
import org.jbd.backend.community.repository.PostRepository;
import org.jbd.backend.community.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository, PostRepository postRepository) {
        this.categoryRepository = categoryRepository;
        this.postRepository = postRepository;
    }

    @Override
    public CategoryDto.Response createCategory(CategoryDto.Request request) {
        Category category = new Category(request.getName(), request.getDescription());
        
        if (request.getDisplayOrder() != null) {
            category.updateDisplayOrder(request.getDisplayOrder());
        }
        
        Category savedCategory = categoryRepository.save(category);
        return new CategoryDto.Response(savedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto.Response getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        return new CategoryDto.Response(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto.Response> getAllActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByDisplayOrder()
                .stream()
                .map(category -> {
                    Long postCount = postRepository.countByCategoryAndIsDeletedFalse(category);
                    return new CategoryDto.Response(category, postCount);
                })
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDto.Response updateCategory(Long id, CategoryDto.Request request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        
        category.update(request.getName(), request.getDescription());
        
        if (request.getDisplayOrder() != null) {
            category.updateDisplayOrder(request.getDisplayOrder());
        }
        
        Category savedCategory = categoryRepository.save(category);
        return new CategoryDto.Response(savedCategory);
    }

    @Override
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        
        category.deactivate();
        categoryRepository.save(category);
    }

    @Override
    public CategoryDto.Response updateDisplayOrder(Long id, Integer displayOrder) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        
        category.updateDisplayOrder(displayOrder);
        Category savedCategory = categoryRepository.save(category);
        return new CategoryDto.Response(savedCategory);
    }
}