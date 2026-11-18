
package com.shopmind.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"category", "images"})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "amazon_asin", length = 20)
    private String amazonAsin;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String brand;

    private String store;


    @Column(nullable = false,precision = 10,scale = 2)
    private BigDecimal price;

    @Builder.Default
    private Integer stock = 0;

    @Builder.Default
    private Boolean active = true;

    @Column(name = "image_url",columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "amazon_rating",precision = 3,scale = 2)
    private BigDecimal amazonRating;

    @Column(name = "rating_count")
    private Integer ratingCount;

    @Column(name = "date_first_available")
    private LocalDate dateFirstAvailable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "product",cascade = CascadeType.ALL,orphanRemoval = true,fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    @CreationTimestamp
    @Column( name = "created_at",updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void addImage(ProductImage image) {

        if (images == null) {
            images = new ArrayList<>();
        }

        images.add(image);
        image.setProduct(this);

        if (image.getPosition() != null && image.getPosition() == 1) {
            this.imageUrl = image.getImageUrl();
        }
    }

    public void removeImage(ProductImage image) {

        images.remove(image);
        image.setProduct(null);

        if (image.getPosition() != null && image.getPosition() == 1) {
            this.imageUrl = null;
        }
    }
}
