
package com.shopmind.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImageResponse {

    private Long id;

    private String imageUrl;

    private Integer position;

    private String variant;

    private String sourceResolution;
}
