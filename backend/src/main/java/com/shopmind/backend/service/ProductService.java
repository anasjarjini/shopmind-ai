package com.shopmind.backend.service;

import com.shopmind.backend.dto.ProductCardResponse;
import com.shopmind.backend.dto.ProductImageRequest;
import com.shopmind.backend.dto.ProductImageResponse;
import com.shopmind.backend.dto.ProductRequest;
import com.shopmind.backend.dto.ProductResponse;
import com.shopmind.backend.entity.Category;
import com.shopmind.backend.entity.Product;
import com.shopmind.backend.entity.ProductImage;
import com.shopmind.backend.repository.CategoryRepository;
import com.shopmind.backend.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    // POST /api/products : create a product with its three images.
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {

        validateRequest(request);

        String asin = normalizeAsin(request.getAmazonAsin());
        checkUniqueAsin(asin, null);
        Category category = findCategory(request.getCategory());

        Product product = Product.builder()
                .amazonAsin(asin)
                .name(request.getName().trim())
                .description(request.getDescription())
                .brand(request.getBrand().trim())
                .store(request.getStore())
                .price(request.getPrice())
                .stock(request.getStock() == null ? 0 : request.getStock())
                .active(request.getActive() == null ? true : request.getActive())
                .category(category)
                .amazonRating(request.getAmazonRating())
                .ratingCount(request.getRatingCount())
                .dateFirstAvailable(parseDate(request.getDateFirstAvailable()))
                .build();

        addImages(product, request.getImages());
        return toResponse(productRepository.saveAndFlush(product));
    }

    // PUT /api/products/{id} : full update including the three images.
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        validateRequest(request);

        Product product = productRepository.findWithImagesById(id).orElseThrow(() -> notFound("Product not found: " + id));

        String asin = normalizeAsin(request.getAmazonAsin());
        checkUniqueAsin(asin, id);
        Category category = findCategory(request.getCategory());

        product.setAmazonAsin(asin);
        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setBrand(request.getBrand().trim());
        product.setStore(request.getStore());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock() == null ? 0 : request.getStock());
        product.setActive(request.getActive() == null ? true : request.getActive());
        product.setCategory(category);
        product.setAmazonRating(request.getAmazonRating());
        product.setRatingCount(request.getRatingCount());
        product.setDateFirstAvailable(parseDate(request.getDateFirstAvailable()));

        // Remove the old gallery first. Flush before inserting replacement
        // images to avoid conflicts with UNIQUE(product_id, position) and URL.
        product.getImages().clear();
        productRepository.flush();
        addImages(product, request.getImages());

        return toResponse(productRepository.saveAndFlush(product));
    }

    // GET /api/products/{id} : full details + all three images.
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findWithImagesById(id).orElseThrow(() -> notFound("Product not found: " + id));
        return toResponse(product);
    }

    // GET /api/products?page=0&size=12&categoryId=...
    @Transactional(readOnly = true)
    public Page<ProductCardResponse> getProductCards(Long categoryId, int page, int size) {

        if (page < 0 || size < 1 || size > 48) {
            throw badRequest("page must be >= 0 and size must be between 1 and 48");
        }
        return productRepository.findProductCards(categoryId, PageRequest.of(page, size));
    }

    // DELETE /api/products/{id} : hard delete, including images by cascade.
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id).orElseThrow(() -> notFound("Product not found: " + id));
        productRepository.delete(product);
    }



    @Transactional(readOnly = true)
    public Page<ProductCardResponse> searchProductCards(String name,int page,int size) {
        
        if (name == null || name.isBlank()) {
            throw badRequest("Search name is required");
        }

        if (page < 0 || size < 1 || size > 48) {
            throw badRequest(
                "Invalid pagination parameters"
            );
        }

        return productRepository.searchProductCards(
            name.trim(),
            PageRequest.of(page, size)
        );
    }


    private Category findCategory(String categoryName) {
        return categoryRepository.findByNameIgnoreCase(categoryName.trim()).orElseThrow(() -> notFound("Category not found: " + categoryName));
    }

    private void checkUniqueAsin(String asin, Long currentProductId) {
        if (asin == null) {
            return;
        }
        productRepository.findByAmazonAsin(asin).ifPresent(existing -> {
            if (currentProductId == null || !existing.getId().equals(currentProductId)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Amazon ASIN already exists: " + asin);
            }
        });
    }

    private String normalizeAsin(String value) {
        if (value == null || value.isBlank()) {
            return null; // Manually created products may have no Amazon ASIN.
        }
        String asin = value.trim().toUpperCase(Locale.ROOT);
        if (!asin.matches("[A-Z0-9]{10}")) {
            throw badRequest("amazonAsin must contain 10 letters/digits");
        }
        return asin;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String date = value.trim();
        try {
            return LocalDate.parse(date); // 2023-01-10
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(
                        date,
                        DateTimeFormatter.ofPattern("MMMM d, uuuu", Locale.ENGLISH));
            } catch (DateTimeParseException ex) {
                throw badRequest(
                        "Invalid dateFirstAvailable; expected 2023-01-10 or January 10, 2023");
            }
        }
    }

    private void validateRequest(ProductRequest request) {
        if (request == null) {
            throw badRequest("ProductRequest is required");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw badRequest("Product name is required");
        }
        if (request.getBrand() == null || request.getBrand().isBlank()) {
            throw badRequest("Brand is required");
        }
        if (request.getCategory() == null || request.getCategory().isBlank()) {
            throw badRequest("Category is required");
        }
        if (request.getPrice() == null ||
                request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw badRequest("Price must be greater than zero");
        }
        if (request.getStock() != null && request.getStock() < 0) {
            throw badRequest("Stock cannot be negative");
        }
        if (request.getAmazonRating() != null &&
                (request.getAmazonRating().compareTo(BigDecimal.ZERO) < 0 ||
                 request.getAmazonRating().compareTo(new BigDecimal("5")) > 0)) {
            throw badRequest("Amazon rating must be between 0 and 5");
        }
        if (request.getRatingCount() != null && request.getRatingCount() < 0) {
            throw badRequest("Rating count cannot be negative");
        }
        if (request.getImages() == null || request.getImages().size() != 3) {
            throw badRequest("A product must have exactly 3 images");
        }

        Set<Integer> positions = new HashSet<>();
        Set<String> urls = new HashSet<>();
        for (ProductImageRequest image : request.getImages()) {
            if (image == null || image.getPosition() == null ||
                    image.getImageUrl() == null || image.getImageUrl().isBlank()) {
                throw badRequest("Every image needs a position and an imageUrl");
            }
            if (image.getPosition() < 1 || image.getPosition() > 3 ||
                    !positions.add(image.getPosition())) {
                throw badRequest("Image positions must be unique: 1, 2, 3");
            }
            if (!urls.add(image.getImageUrl().trim())) {
                throw badRequest("Image URLs must be different");
            }
        }
        if (!positions.equals(Set.of(1, 2, 3))) {
            throw badRequest("Image positions must be exactly 1, 2 and 3");
        }
    }

    private void addImages(Product product, List<ProductImageRequest> requests) {
        requests.stream().sorted(Comparator.comparingInt(ProductImageRequest::getPosition))
                .forEach(request -> {
                    ProductImage image = ProductImage.builder()
                            .imageUrl(request.getImageUrl().trim())
                            .position(request.getPosition())
                            .variant(request.getVariant())
                            .sourceResolution(request.getSourceResolution())
                            .build();
                    product.addImage(image);
                });
    }

    private ProductResponse toResponse(Product product) {
        List<ProductImageResponse> images = product.getImages().stream()
                .sorted(Comparator.comparingInt(ProductImage::getPosition))
                .map(image -> ProductImageResponse.builder()
                        .id(image.getId())
                        .imageUrl(image.getImageUrl())
                        .position(image.getPosition())
                        .variant(image.getVariant())
                        .sourceResolution(image.getSourceResolution())
                        .build())
                .toList();

        Category category = product.getCategory();
        return ProductResponse.builder()
                .id(product.getId())
                .amazonAsin(product.getAmazonAsin())
                .name(product.getName())
                .description(product.getDescription())
                .brand(product.getBrand())
                .store(product.getStore())
                .price(product.getPrice())
                .stock(product.getStock())
                .active(product.getActive())
                .categoryId(category == null ? null : category.getId())
                .categoryName(category == null ? null : category.getName())
                .amazonRating(product.getAmazonRating())
                .ratingCount(product.getRatingCount())
                .dateFirstAvailable(product.getDateFirstAvailable())
                .images(images)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
