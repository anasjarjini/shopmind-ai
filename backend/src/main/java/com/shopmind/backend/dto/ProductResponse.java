
package com.shopmind.backend.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    // PRODUCT IDENTITY
    private Long id;
    private String amazonAsin;

    // PRODUCT INFORMATION
    private String name;
    private String description;
    private String brand;
    private String store;

    // PRICE AND AVAILABILITY
    private BigDecimal price;
    private Integer stock;
    private Boolean active;

    // CATEGORY
    private Long categoryId;
    private String categoryName;

    // AMAZON METADATA
    private BigDecimal amazonRating;
    private Integer ratingCount;
    private LocalDate dateFirstAvailable;

    // PRODUCT GALLERY (3 IMAGES)
    @Builder.Default
    private List<ProductImageResponse> images = new ArrayList<>();

    // DATABASE TIMESTAMPS
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
