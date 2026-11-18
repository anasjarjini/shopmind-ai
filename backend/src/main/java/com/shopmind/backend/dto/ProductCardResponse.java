
package com.shopmind.backend.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCardResponse {

    private Long id;

    private String name;

    private String brand;

    private BigDecimal price;

    private Integer stock;

    private Boolean active;

    private Long categoryId;

    private String categoryName;

    private String mainImageUrl;

    private BigDecimal amazonRating;

    private Integer ratingCount;
}
