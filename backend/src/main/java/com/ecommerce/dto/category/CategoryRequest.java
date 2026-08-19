package com.ecommerce.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Admin payload for creating or updating a category. */
public record CategoryRequest(

        @NotBlank(message = "Category name is required")
        @Size(max = 100, message = "Category name must not exceed 100 characters")
        String name,

        /*
         * Optional: CategoryService derives a slug from the name when this is blank.
         * Restricted to lowercase, digits and hyphens so it is always URL-safe.
         */
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$|^$",
                 message = "Slug may contain only lowercase letters, numbers and hyphens")
        @Size(max = 120, message = "Slug must not exceed 120 characters")
        String slug,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @Size(max = 500, message = "Image URL must not exceed 500 characters")
        String imageUrl
) {}
