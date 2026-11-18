
package com.shopmind.backend.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageRequest {

    @NotBlank
    private String imageUrl;

    @NotNull
    @Min(1)
    @Max(3)
    private Integer position;

    private String variant;

    private String sourceResolution;
}
