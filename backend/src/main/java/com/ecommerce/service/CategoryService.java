package com.ecommerce.service;

import com.ecommerce.dto.category.CategoryRequest;
import com.ecommerce.dto.category.CategoryResponse;
import com.ecommerce.entity.Category;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.DuplicateResourceException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(category -> CategoryResponse.from(category,
                        productRepository.countByCategoryId(category.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("No category found with slug: " + slug));
        return CategoryResponse.from(category, productRepository.countByCategoryId(category.getId()));
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        Category category = findCategoryOrThrow(id);
        return CategoryResponse.from(category, productRepository.countByCategoryId(id));
    }

    @Transactional(readOnly = true)
    public Category findCategoryOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();

        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("A category named '" + name + "' already exists.");
        }

        String slug = resolveSlug(request.slug(), name);
        if (categoryRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("A category with the URL '" + slug + "' already exists.");
        }

        Category category = Category.builder()
                .name(name)
                .slug(slug)
                .description(trimToNull(request.description()))
                .imageUrl(trimToNull(request.imageUrl()))
                .build();

        Category saved = categoryRepository.save(category);
        log.info("Admin created category '{}' (id={})", saved.getName(), saved.getId());
        return CategoryResponse.from(saved, 0L);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findCategoryOrThrow(id);
        String name = request.name().trim();

        Optional<Category> existingByName = categoryRepository.findByNameIgnoreCase(name);
        if (existingByName.isPresent() && !existingByName.get().getId().equals(id)) {
            throw new DuplicateResourceException("A category named '" + name + "' already exists.");
        }

        String slug = resolveSlug(request.slug(), name);
        Optional<Category> existingBySlug = categoryRepository.findBySlug(slug);
        if (existingBySlug.isPresent() && !existingBySlug.get().getId().equals(id)) {
            throw new DuplicateResourceException("A category with the URL '" + slug + "' already exists.");
        }

        category.setName(name);
        category.setSlug(slug);
        category.setDescription(trimToNull(request.description()));
        category.setImageUrl(trimToNull(request.imageUrl()));

        Category saved = categoryRepository.save(category);
        log.info("Admin updated category id={}", id);
        return CategoryResponse.from(saved, productRepository.countByCategoryId(id));
    }

    @Transactional
    public void delete(Long id) {
        Category category = findCategoryOrThrow(id);
        long productCount = productRepository.countByCategoryId(id);

        if (productCount > 0) {
            throw new BadRequestException(
                    "Cannot delete '%s' because %d product(s) still belong to it. Move or delete those products first."
                            .formatted(category.getName(), productCount));
        }

        categoryRepository.delete(category);
        log.info("Admin deleted category id={}", id);
    }

    private String resolveSlug(String requestedSlug, String name) {
        String slug = (requestedSlug == null || requestedSlug.isBlank())
                ? SlugUtils.toSlug(name)
                : SlugUtils.toSlug(requestedSlug);

        if (slug.isBlank()) {
            throw new BadRequestException("Could not derive a valid URL slug from the category name.");
        }
        return slug;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
