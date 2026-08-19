package com.ecommerce.config;

import com.ecommerce.entity.*;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.RoleRepository;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Populates a brand-new database with the data the application cannot run without
 * (the two roles, the six required categories, an admin account) plus a realistic starter
 * catalogue.
 *
 * <p>Every step is <b>idempotent</b> - it checks before inserting - so restarting the
 * application never duplicates rows or overwrites an admin's edits.
 *
 * <p>The seeded products are genuine database rows: the admin can edit, restock or delete
 * them, and customers order them like anything else. They are starting inventory, not
 * hard-coded frontend placeholders.
 */
@Component
@RequiredArgsConstructor
@Order(1)
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminSeedProperties adminProperties;

    @Override
    @Transactional
    public void run(String... args) {
        seedRoles();
        seedAdminUser();
        seedCategories();
        seedProducts();
    }

    // ------------------------------------------------------------------

    private void seedRoles() {
        for (RoleName roleName : RoleName.values()) {
            roleRepository.findByName(roleName).orElseGet(() -> {
                log.info("Seeding role {}", roleName);
                return roleRepository.save(new Role(roleName));
            });
        }
    }

    /**
     * Creates the bootstrap administrator.
     *
     * <p>The password is read from {@code ADMIN_PASSWORD} and BCrypt-hashed before storage.
     * If that variable is absent, seeding is skipped with a warning rather than falling
     * back to a default password - a well-known default admin password would be a far
     * worse outcome than having no admin yet.
     */
    private void seedAdminUser() {
        String email = adminProperties.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            return;
        }

        String password = adminProperties.getPassword();
        if (password == null || password.isBlank()) {
            log.warn("""

                    ------------------------------------------------------------
                     ADMIN_PASSWORD is not set, so no administrator was created.
                     Set it in backend/.env and restart:
                       ADMIN_PASSWORD=<a strong password>
                    ------------------------------------------------------------
                    """);
            return;
        }

        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN was not seeded"));

        User admin = User.builder()
                .fullName(adminProperties.getFullName())
                .email(email)
                .password(passwordEncoder.encode(password))
                .phoneNumber(adminProperties.getPhoneNumber())
                .enabled(true)
                .roles(Set.of(adminRole))
                .build();

        userRepository.save(admin);
        log.info("Seeded administrator account: {}", email);
    }

    /** The six required product sections. */
    private void seedCategories() {
        record Seed(String name, String slug, String description, String imageUrl) {}

        List<Seed> seeds = List.of(
                new Seed("Books", "books",
                        "Fiction, non-fiction, academic titles and more.",
                        "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=600&q=80"),
                new Seed("Grocery", "grocery",
                        "Everyday staples, snacks, beverages and household essentials.",
                        "https://images.unsplash.com/photo-1542838132-92c53300491e?w=600&q=80"),
                new Seed("Kitchen Utensils", "kitchen-utensils",
                        "Cookware, tools and gadgets for every kitchen.",
                        "https://images.unsplash.com/photo-1556911220-bff31c812dba?w=600&q=80"),
                new Seed("Clothes", "clothes",
                        "Everyday and occasion wear for men, women and children.",
                        "https://images.unsplash.com/photo-1489987707025-afc232f7ea0f?w=600&q=80"),
                new Seed("Electronics", "electronics",
                        "Phones, audio, computing and smart devices.",
                        "https://images.unsplash.com/photo-1498049794561-7780e7231661?w=600&q=80"),
                new Seed("Furniture", "furniture",
                        "Seating, storage and workspace furniture for your home.",
                        "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=600&q=80")
        );

        for (Seed seed : seeds) {
            if (categoryRepository.findBySlug(seed.slug()).isEmpty()) {
                categoryRepository.save(Category.builder()
                        .name(seed.name())
                        .slug(seed.slug())
                        .description(seed.description())
                        .imageUrl(seed.imageUrl())
                        .build());
                log.info("Seeded category {}", seed.name());
            }
        }
    }

    /**
     * Starter inventory across all six categories.
     *
     * <p>Only runs when the products table is empty, so an admin who deletes a seeded
     * product will not find it resurrected on the next restart.
     */
    private void seedProducts() {
        if (productRepository.count() > 0) {
            return;
        }

        seedCategoryProducts("books", List.of(
                new P("The Silent Patient", "A gripping psychological thriller about a woman who stops speaking after a shocking act of violence.", "399.00", "Orion", 40, "https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=600&q=80"),
                new P("Atomic Habits", "A practical guide to building good habits and breaking bad ones, one small change at a time.", "549.00", "Random House", 65, "https://images.unsplash.com/photo-1592496431122-2349e0fbc666?w=600&q=80"),
                new P("Clean Code", "A handbook of agile software craftsmanship, essential reading for working programmers.", "2899.00", "Prentice Hall", 22, "https://images.unsplash.com/photo-1532012197267-da84d127e765?w=600&q=80"),
                new P("Sapiens: A Brief History of Humankind", "A sweeping account of how Homo sapiens came to dominate the planet.", "699.00", "Harper", 33, "https://images.unsplash.com/photo-1541963463532-d68292c34b19?w=600&q=80"),
                new P("Introduction to Algorithms", "The comprehensive reference on algorithms used in computer science courses worldwide.", "4250.00", "MIT Press", 12, "https://images.unsplash.com/photo-1517842645767-c639042777db?w=600&q=80"),
                new P("The Alchemist", "A modern fable about following your dreams, translated into more than eighty languages.", "349.00", "HarperOne", 58, "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=600&q=80"),
                new P("Deep Work", "Rules for focused success in a distracted world.", "499.00", "Grand Central", 27, "https://images.unsplash.com/photo-1512045482940-f37f5216f639?w=600&q=80")
        ));

        seedCategoryProducts("grocery", List.of(
                new P("Organic Basmati Rice 5kg", "Long-grain aged basmati rice, naturally aromatic and sourced from certified organic farms.", "749.00", "Nature Fresh", 90, "https://images.unsplash.com/photo-1586201375761-83865001e31c?w=600&q=80"),
                new P("Cold Pressed Olive Oil 1L", "Extra virgin olive oil, first cold pressing, ideal for dressings and finishing.", "899.00", "Mediterra", 45, "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=600&q=80"),
                new P("Arabica Coffee Beans 500g", "Medium-roast single-origin arabica beans with notes of chocolate and citrus.", "649.00", "Bean & Brew", 52, "https://images.unsplash.com/photo-1447933601403-0c6688de566e?w=600&q=80"),
                new P("Raw Forest Honey 500g", "Unprocessed, unpasteurised honey collected from forest beehives.", "459.00", "HiveCraft", 38, "https://images.unsplash.com/photo-1587049352846-4a222e784d38?w=600&q=80"),
                new P("Assorted Dry Fruits 1kg", "A premium mix of almonds, cashews, pistachios and raisins.", "1299.00", "NutriPick", 30, "https://images.unsplash.com/photo-1599599810769-bcde5a160d32?w=600&q=80"),
                new P("Green Tea Bags (100 count)", "Whole-leaf green tea in biodegradable pyramid bags.", "399.00", "Leaf Origin", 74, "https://images.unsplash.com/photo-1556881286-fc6915169721?w=600&q=80"),
                new P("Whole Wheat Atta 10kg", "Stone-ground whole wheat flour with the bran retained.", "525.00", "Nature Fresh", 4, "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=600&q=80")
        ));

        seedCategoryProducts("kitchen-utensils", List.of(
                new P("Triply Stainless Steel Cookware Set", "Five-piece induction-friendly set with an aluminium core for even heating.", "4999.00", "ChefLine", 18, "https://images.unsplash.com/photo-1584990347449-a40e2b0e8b0f?w=600&q=80"),
                new P("Cast Iron Skillet 10 inch", "Pre-seasoned cast iron pan that improves with every use. Oven safe.", "1899.00", "IronCraft", 26, "https://images.unsplash.com/photo-1593618998160-e34014e67546?w=600&q=80"),
                new P("Professional Chef Knife 8 inch", "High-carbon stainless steel blade with a full-tang balanced handle.", "2450.00", "EdgeMaster", 21, "https://images.unsplash.com/photo-1593618998160-e34014e67546?w=600&q=80"),
                new P("Bamboo Chopping Board Set", "Three-piece board set in renewable bamboo with juice grooves.", "999.00", "GreenLeaf", 44, "https://images.unsplash.com/photo-1594385208974-2e75f8d7bb48?w=600&q=80"),
                new P("Silicone Spatula Set (6 piece)", "Heat-resistant to 260C, non-scratch and dishwasher safe.", "749.00", "ChefLine", 60, "https://images.unsplash.com/photo-1556910103-1c02745aae4d?w=600&q=80"),
                new P("Stainless Steel Pressure Cooker 5L", "ISI-certified cooker with a precision weight valve and induction base.", "2699.00", "HomeChef", 15, "https://images.unsplash.com/photo-1585515320310-259814833e62?w=600&q=80"),
                new P("Glass Food Storage Containers (10 piece)", "Borosilicate glass with airtight leak-proof lids. Freezer to oven safe.", "1799.00", "PureStore", 3, "https://images.unsplash.com/photo-1584990347449-a40e2b0e8b0f?w=600&q=80")
        ));

        seedCategoryProducts("clothes", List.of(
                new P("Classic Cotton Oxford Shirt", "Breathable long-staple cotton with a button-down collar. Everyday smart-casual.", "1499.00", "Northfield", 55, "https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=600&q=80"),
                new P("Slim Fit Denim Jeans", "Mid-rise stretch denim that keeps its shape through the day.", "2199.00", "Denim Co", 48, "https://images.unsplash.com/photo-1542272604-787c3835535d?w=600&q=80"),
                new P("Merino Wool Crew Sweater", "Fine-gauge merino that regulates temperature without bulk.", "3299.00", "Highland", 24, "https://images.unsplash.com/photo-1576871337622-98d48d1cf531?w=600&q=80"),
                new P("Lightweight Running Jacket", "Wind-resistant and water-repellent with reflective detailing.", "2799.00", "Stride", 31, "https://images.unsplash.com/photo-1591047139829-d91aecb6caea?w=600&q=80"),
                new P("Cotton Graphic T-Shirt", "Soft combed cotton with a screen print that survives the wash.", "699.00", "Urban Thread", 88, "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=600&q=80"),
                new P("Ethnic Silk Blend Kurta", "Handloom-inspired weave with subtle self-texture, ideal for occasions.", "1899.00", "Rangrez", 37, "https://images.unsplash.com/photo-1610030469983-98e550d6193c?w=600&q=80"),
                new P("Everyday Canvas Sneakers", "Vulcanised rubber sole with a cushioned cotton-twill insole.", "1599.00", "Stride", 42, "https://images.unsplash.com/photo-1560769629-975ec94e6a86?w=600&q=80")
        ));

        seedCategoryProducts("electronics", List.of(
                new P("Wireless Noise Cancelling Headphones", "Over-ear ANC headphones with 30-hour battery life and multipoint pairing.", "12999.00", "AudioPeak", 25, "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600&q=80"),
                new P("Smartphone 5G 128GB", "6.5-inch AMOLED display, 50MP main camera and all-day 5000mAh battery.", "24999.00", "Nexon", 19, "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=600&q=80"),
                new P("14 inch Ultrabook Laptop", "16GB RAM, 512GB NVMe SSD and a backlit keyboard in a 1.3kg chassis.", "68999.00", "Corvex", 9, "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=600&q=80"),
                new P("Smart Fitness Watch", "Continuous heart-rate, SpO2 and sleep tracking with a seven-day battery.", "4999.00", "PulseFit", 47, "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&q=80"),
                new P("Portable Bluetooth Speaker", "IPX7 waterproof speaker with 12 hours of playback and deep bass.", "3499.00", "AudioPeak", 53, "https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?w=600&q=80"),
                new P("Mechanical Keyboard RGB", "Hot-swappable tactile switches, aluminium frame and per-key lighting.", "6499.00", "KeyForge", 28, "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=600&q=80"),
                new P("1080p Webcam with Ring Light", "Auto-focus webcam with a built-in adjustable ring light and privacy shutter.", "2999.00", "Corvex", 5, "https://images.unsplash.com/photo-1587826080692-f439cd0b70da?w=600&q=80")
        ));

        seedCategoryProducts("furniture", List.of(
                new P("Ergonomic Mesh Office Chair", "Breathable mesh back with adjustable lumbar support and 4D armrests.", "12499.00", "WorkWell", 16, "https://images.unsplash.com/photo-1580480055273-228ff5388ef8?w=600&q=80"),
                new P("Solid Wood Study Desk", "Sheesham wood desk with a cable channel and two storage drawers.", "15999.00", "TimberCraft", 8, "https://images.unsplash.com/photo-1518455027359-f3f8164ba6bd?w=600&q=80"),
                new P("Three Seater Fabric Sofa", "High-resilience foam cushioning with a stain-resistant woven cover.", "34999.00", "HomeNest", 6, "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=600&q=80"),
                new P("Five Tier Bookshelf", "Engineered wood shelving with anti-tip wall anchors included.", "6999.00", "TimberCraft", 23, "https://images.unsplash.com/photo-1594620302200-9a762244a156?w=600&q=80"),
                new P("Queen Size Bed Frame", "Upholstered platform bed with slat support - no box spring required.", "27999.00", "HomeNest", 7, "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=600&q=80"),
                new P("Nesting Coffee Table Set", "Two-piece set in powder-coated steel with tempered glass tops.", "8999.00", "UrbanForm", 14, "https://images.unsplash.com/photo-1533090161767-e6ffed986c88?w=600&q=80"),
                new P("Adjustable Standing Desk Converter", "Gas-spring riser that lifts a monitor and keyboard to standing height.", "9499.00", "WorkWell", 2, "https://images.unsplash.com/photo-1593062096033-9a26b09da705?w=600&q=80")
        ));

        log.info("Seeded {} starter products", productRepository.count());
    }

    /** One row of starter inventory. Price is a string so it can become an exact BigDecimal. */
    private record P(String name, String description, String price, String brand, int stock, String imageUrl) {}

    private void seedCategoryProducts(String categorySlug, List<P> products) {
        Category category = categoryRepository.findBySlug(categorySlug).orElse(null);
        if (category == null) {
            log.warn("Category '{}' is missing; skipping its product seed", categorySlug);
            return;
        }

        for (P p : products) {
            productRepository.save(Product.builder()
                    .name(p.name())
                    .description(p.description())
                    // Constructed from a string, never a double - money must be exact.
                    .price(new BigDecimal(p.price()))
                    .category(category)
                    .brand(p.brand())
                    .imageUrl(p.imageUrl())
                    .stock(p.stock())
                    .active(true)
                    .build());
        }
    }
}
