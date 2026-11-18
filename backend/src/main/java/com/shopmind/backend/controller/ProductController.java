
package com.shopmind.backend.controller;

import com.shopmind.backend.dto.ProductCardResponse;
import com.shopmind.backend.dto.ProductRequest;
import com.shopmind.backend.dto.ProductResponse;
import com.shopmind.backend.service.ProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.shopmind.backend.dto.RecommendedProductResponse;
import com.shopmind.backend.service.ProductRecommendationService;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductRecommendationService productRecommendationService;

    @GetMapping
    public ResponseEntity<Page<ProductCardResponse>> getProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ResponseEntity.ok(productService.getProductCards(categoryId, page, size));
    }


        @GetMapping("/{id}/recommendations")
        public ResponseEntity<List<RecommendedProductResponse>>
        getRecommendations(@PathVariable Long id,@RequestParam(defaultValue = "5") int limit,@RequestParam(defaultValue = "true")boolean sameCategory) {

            return ResponseEntity.ok(
                productRecommendationService.getRecommendations(id,limit,sameCategory)
            );
        }



    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }


    @GetMapping("/search")
    public ResponseEntity<Page<ProductCardResponse>> searchProducts(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ResponseEntity.ok(
            productService.searchProductCards(name, page, size)
        );
    }


    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<ProductCardResponse>> getProductsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ResponseEntity.ok(productService.getProductCards(categoryId, page, size));
    }


    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }


    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request
    ) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
