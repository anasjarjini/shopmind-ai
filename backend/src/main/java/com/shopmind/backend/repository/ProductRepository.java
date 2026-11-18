package com.shopmind.backend.repository;
import com.shopmind.backend.dto.ProductCardResponse;
import com.shopmind.backend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByActiveTrue();

    List<Product> findByCategoryId(Long categoryId);

    List<Product> findByCategoryIdAndActiveTrue(Long categoryId);

    List<Product> findByNameContainingIgnoreCase(String name);

    List<Product> findByBrandIgnoreCase(String brand);

    List<Product> findByPriceBetween(BigDecimal minPrice,BigDecimal maxPrice);

    boolean existsByCategoryId(Long categoryId);

    Optional<Product> findByAmazonAsin(String amazonAsin);

    boolean existsByAmazonAsin(String amazonAsin);

   
    @Query(
        value = """
            SELECT new com.shopmind.backend.dto.ProductCardResponse(
                p.id,
                p.name,
                p.brand,
                p.price,
                p.stock,
                p.active,
                c.id,
                c.name,
                i.imageUrl,
                p.amazonRating,
                p.ratingCount
            )
            FROM Product p
            LEFT JOIN p.category c
            LEFT JOIN p.images i ON i.position = 1
            WHERE p.active = true
            AND (:categoryId IS NULL OR c.id = :categoryId)
            ORDER BY p.id DESC
        """,
        countQuery = """
            SELECT COUNT(p)
            FROM Product p
            WHERE p.active = true
            AND (:categoryId IS NULL
                OR p.category.id = :categoryId)
        """
    )
    Page<ProductCardResponse> findProductCards(@Param("categoryId") Long categoryId,Pageable pageable);


    @Query(
        value = """
            SELECT new com.shopmind.backend.dto.ProductCardResponse(
                p.id,
                p.name,
                p.brand,
                p.price,
                p.stock,
                p.active,
                c.id,
                c.name,
                i.imageUrl,
                p.amazonRating,
                p.ratingCount
            )
            FROM Product p
            LEFT JOIN p.category c
            LEFT JOIN p.images i ON i.position = 1
            WHERE p.active = true
            AND LOWER(p.name) LIKE
                LOWER(CONCAT('%', :name, '%'))
            ORDER BY p.id DESC
        """,
        countQuery = """
            SELECT COUNT(p)
            FROM Product p
            WHERE p.active = true
            AND LOWER(p.name) LIKE
                LOWER(CONCAT('%', :name, '%'))
        """
    )
    Page<ProductCardResponse> searchProductCards( @Param("name") String name, Pageable pageable);




    @Query("""
        SELECT DISTINCT p
        FROM Product p
        LEFT JOIN FETCH p.images
        LEFT JOIN FETCH p.category
        WHERE p.id = :productId
    """)
    Optional<Product> findWithImagesById(
            @Param("productId") Long productId
    );

    @EntityGraph(attributePaths = {"category"})
    List<Product> findAllByActiveTrue();


    @Query("""
        SELECT new com.shopmind.backend.dto.ProductCardResponse(
            p.id,
            p.name,
            p.brand,
            p.price,
            p.stock,
            p.active,
            c.id,
            c.name,
            i.imageUrl,
            p.amazonRating,
            p.ratingCount
        )
        FROM Product p
        LEFT JOIN p.category c
        LEFT JOIN p.images i ON i.position = 1
        WHERE p.active = true
        AND p.id IN :productIds
    """)
    List<ProductCardResponse> findActiveCardsByIds(
        @Param("productIds") Collection<Long> productIds
    );

}
