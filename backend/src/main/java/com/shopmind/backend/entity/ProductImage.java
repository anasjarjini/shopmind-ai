
package com.shopmind.backend.entity;
import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "product_images",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_product_image_position",columnNames = {"product_id", "position"}),
        @UniqueConstraint(name = "uq_product_image_url",columnNames = {"product_id", "image_url"})
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "product")
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    @Column(nullable = false)
    private Integer position;

    @Column(length = 20)
    private String variant;

    @Column(name = "source_resolution", length = 20)
    private String sourceResolution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id",nullable = false)
    @JsonIgnore
    private Product product;

}
