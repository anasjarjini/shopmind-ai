
package com.shopmind.backend.service;

import com.shopmind.backend.dto.CategoryRequest;
import com.shopmind.backend.dto.CategoryResponse;
import com.shopmind.backend.entity.Category;
import com.shopmind.backend.repository.CategoryRepository;
import com.shopmind.backend.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;


@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    //Récupérer toutes les catégories
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream().map(this::toResponse).toList();
    }


    // 2. Récupérer une catégorie par ID
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = findCategory(id);
        return toResponse(category);
    }


    //Créer une catégorie
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String name = request.getName().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException( HttpStatus.CONFLICT,"Category already exists"
            );
        }

        Category category = Category.builder().name(name).description(request.getDescription()).build();
        Category savedCategory = categoryRepository.save(category);
        return toResponse(savedCategory);
    }

    //Modifier une catégorie
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {

        Category category = findCategory(id);
        String name = request.getName().trim();

        if (!category.getName().equalsIgnoreCase(name) && categoryRepository.existsByNameIgnoreCase(name)) {

            throw new ResponseStatusException(HttpStatus.CONFLICT,"Category already exists");
        }


        category.setName(name);
        category.setDescription(request.getDescription());
        Category updatedCategory = categoryRepository.save(category);
        return toResponse(updatedCategory);
    }


    //Supprimer une catégorie
    @Transactional
    public void deleteCategory(Long id) {

        Category category = findCategory(id);

        if (productRepository.existsByCategoryId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Cannot delete category containing products");
        }

        categoryRepository.delete(category);
    }

    //Chercher la catégorie ou retourner 404
    private Category findCategory(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new ResponseStatusException( HttpStatus.NOT_FOUND, "Category not found: " + id));
    }

    //Convertir Entity -> DTO
    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder().id(category.getId()).name(category.getName()).description(category.getDescription()).build();
    }
}
