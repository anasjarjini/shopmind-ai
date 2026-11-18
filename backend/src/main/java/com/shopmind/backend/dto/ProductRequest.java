
package com.shopmind.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    private String amazonAsin;

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    @NotBlank(message = "Brand is required")
    private String brand;

    private String store;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01")
    private BigDecimal price;

    @Builder.Default
    @Min(0)
    private Integer stock = 0;

    @Builder.Default
    private Boolean active = true;

    @NotBlank(message = "Category is required")
    private String category;

    @DecimalMin("0.0")
    @DecimalMax("5.0")
    private BigDecimal amazonRating;

    @Min(0)
    private Integer ratingCount;

    private String dateFirstAvailable;

    @NotNull(message = "Images are required")
    @Size(min = 3, max = 3,message = "Product must have exactly 3 images")
    private List<@NotNull @Valid ProductImageRequest> images;
}
