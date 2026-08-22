package com.ecommerce.service;

import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.dto.product.ProductFilterRequest;
import com.ecommerce.dto.product.ProductRequest;
import com.ecommerce.dto.product.ProductResponse;
import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.OrderItemRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.ProductSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    public static final int LOW_STOCK_THRESHOLD = 5;
    private static final int MAX_PAGE_SIZE = 100;

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final OrderItemRepository orderItemRepository;

    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> search(ProductFilterRequest filter, int page, int size,
                                                 boolean includeInactive) {
        int validPage = Math.max(page, 0);
        int validSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(validPage, validSize, resolveSort(filter.sort()));

        Page<Product> results = productRepository.findAll(
                ProductSpecification.withFilters(filter, includeInactive), pageable);

        return PagedResponse.from(results, ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        Product product = findProductOrThrow(id);
        if (!product.isActive()) {
            throw new ResourceNotFoundException("Product", id);
        }
        return ProductResponse.from(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getByIdForAdmin(Long id) {
        return ProductResponse.from(findProductOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Product findProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getFeatured() {
        return productRepository.findTop8ByActiveTrueOrderByAverageRatingDescReviewCountDesc()
                .stream().map(ProductResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getPopular() {
        List<Product> popular =
                productRepository.findTop8ByActiveTrueAndReviewCountGreaterThanOrderByReviewCountDesc(0);

        if (popular.isEmpty()) {
            popular = productRepository.findTop8ByActiveTrueOrderByDateAddedDesc();
        }
        return popular.stream().map(ProductResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getNewArrivals() {
        return productRepository.findTop8ByActiveTrueOrderByDateAddedDesc()
                .stream().map(ProductResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<String> getBrands(Long categoryId) {
        return productRepository.findDistinctBrands(categoryId);
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Category category = categoryService.findCategoryOrThrow(request.categoryId());

        Product product = Product.builder()
                .name(request.name().trim())
                .description(trimToNull(request.description()))
                .price(request.price())
                .category(category)
                .brand(trimToNull(request.brand()))
                .imageUrl(trimToNull(request.imageUrl()))
                .stock(request.stock())
                .active(request.active() == null || request.active())
                .build();

        Product saved = productRepository.save(product);
        log.info("Admin created product '{}' (id={})", saved.getName(), saved.getId());
        return ProductResponse.from(saved);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findProductOrThrow(id);
        Category category = categoryService.findCategoryOrThrow(request.categoryId());

        product.setName(request.name().trim());
        product.setDescription(trimToNull(request.description()));
        product.setPrice(request.price());
        product.setCategory(category);
        product.setBrand(trimToNull(request.brand()));
        product.setImageUrl(trimToNull(request.imageUrl()));
        product.setStock(request.stock());
        if (request.active() != null) {
            product.setActive(request.active());
        }

        Product saved = productRepository.save(product);
        log.info("Admin updated product id={}", id);
        return ProductResponse.from(saved);
    }

    @Transactional
    public ProductResponse updateStock(Long id, Integer stock) {
        if (stock == null || stock < 0) {
            throw new BadRequestException("Stock cannot be negative.");
        }
        Product product = findProductOrThrow(id);
        product.setStock(stock);
        Product saved = productRepository.save(product);
        log.info("Admin set stock of product id={} to {}", id, stock);
        return ProductResponse.from(saved);
    }

    @Transactional
    public boolean delete(Long id) {
        Product product = findProductOrThrow(id);

        if (orderItemRepository.existsByProductId(id)) {
            product.setActive(false);
            productRepository.save(product);
            log.info("Admin deactivated product id={} (referenced by existing orders)", id);
            return true;
        }

        productRepository.delete(product);
        log.info("Admin deleted product id={}", id);
        return false;
    }

    @Transactional(readOnly = true)
    public List<Product> getLowStockProducts() {
        return productRepository.findByActiveTrueAndStockLessThanEqualOrderByStockAsc(LOW_STOCK_THRESHOLD);
    }

    private Sort resolveSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "dateAdded");
        }
        return switch (sort.trim().toLowerCase()) {
            case "price_asc"  -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "rating"     -> Sort.by(Sort.Direction.DESC, "averageRating")
                                     .and(Sort.by(Sort.Direction.DESC, "reviewCount"));
            case "popular"    -> Sort.by(Sort.Direction.DESC, "reviewCount");
            case "name_asc"   -> Sort.by(Sort.Direction.ASC, "name");
            case "name_desc"  -> Sort.by(Sort.Direction.DESC, "name");
            case "oldest"     -> Sort.by(Sort.Direction.ASC, "dateAdded");
            default           -> Sort.by(Sort.Direction.DESC, "dateAdded");
        };
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
