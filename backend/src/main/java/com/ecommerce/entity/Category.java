package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * A product section. The six required sections (Books, Grocery, Kitchen Utensils,
 * Clothes, Electronics, Furniture) are seeded on first run, but admins can add more.
 *
 * <p>{@code slug} is the URL-safe identifier used by the frontend
 * ({@code /category/kitchen-utensils}), so links stay readable and stable even if the
 * display name is edited later.
 */
@Entity
@Table(name = "categories", uniqueConstraints = {
        @UniqueConstraint(name = "uk_categories_name", columnNames = "name"),
        @UniqueConstraint(name = "uk_categories_slug", columnNames = "slug")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 120)
    private String slug;

    @Column(length = 500)
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;
}
