package com.ecommerce.entity;

/**
 * The two authorities in the system.
 *
 * <p>The {@code ROLE_} prefix is Spring Security's convention: {@code hasRole("ADMIN")}
 * checks for an authority literally named {@code ROLE_ADMIN}. Keeping the prefix in the
 * stored name lets us map roles to authorities without string surgery.
 */
public enum RoleName {
    ROLE_CUSTOMER,
    ROLE_ADMIN
}
