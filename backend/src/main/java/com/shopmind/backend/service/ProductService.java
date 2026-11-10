
package com.shopmind.backend.service;

import com.shopmind.backend.dto.ProductRequest;
import com.shopmind.backend.dto.ProductResponse;
import com.shopmind.backend.entity.Product;
import com.shopmind.backend.entity.Category;
import com.shopmind.backend.repository.ProductRepository;
import com.shopmind.backend.repository.CategoryRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;


@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;


    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream().map(this::toResponse).toList();
    }

    //Récupérer un produit par ID
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = findProduct(id);
        return toResponse(product);
    }


    // Rechercher des produits par nom
    @Transactional(readOnly = true)
    public List<ProductResponse> searchProducts(String name) {
        return productRepository.findByNameContainingIgnoreCase(name).stream().map(this::toResponse).toList();
    }


    //Récupérer les produits par catégorie
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByCategory(Long categoryId) {
        findCategory(categoryId);
        return productRepository.findByCategoryId(categoryId).stream().map(this::toResponse).toList();
    }

    // Créer un produit
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Category category = findCategory(request.getCategoryId());
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .brand(request.getBrand())
                .price(request.getPrice())
                .stock(request.getStock())
                .imageUrl(request.getImageUrl())
                .active(request.getActive() != null ? request.getActive() : true)
                .category(category)
                .build();

        Product savedProduct = productRepository.save(product);

        return toResponse(savedProduct);
    }

    // Modifier un produit
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {

        Product product = findProduct(id);
        Category category = findCategory(request.getCategoryId());

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setBrand(request.getBrand());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(category);

        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }

        Product updatedProduct = productRepository.save(product);

        return toResponse(updatedProduct);
    }


    // Supprimer un produit
    @Transactional
    public void deleteProduct(Long id) {
        Product product = findProduct(id);
        productRepository.delete(product);
    }

    // Chercher le produit ou retourner 404
    private Product findProduct(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Product not found: " + id));
    }


    // Chercher la catégorie ou retourner 404
    private Category findCategory(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new ResponseStatusException( HttpStatus.NOT_FOUND, "Category not found: " + id));
    }


    //Convertir Entity -> DTO
    private ProductResponse toResponse(Product product) {

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .brand(product.getBrand())
                .price(product.getPrice())
                .stock(product.getStock())
                .imageUrl(product.getImageUrl())
                .active(product.getActive())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
