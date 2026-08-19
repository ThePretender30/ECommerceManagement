package com.ecommerce.controller;

import com.ecommerce.dto.admin.AdminStatsResponse;
import com.ecommerce.dto.admin.NotificationResponse;
import com.ecommerce.dto.auth.UserResponse;
import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.entity.NotificationStatus;
import com.ecommerce.security.UserPrincipal;
import com.ecommerce.service.AdminStatsService;
import com.ecommerce.service.NotificationService;
import com.ecommerce.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Dashboard statistics, user management and the notification audit log (ROLE_ADMIN). */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin - Dashboard", description = "Statistics, users and notification history (ROLE_ADMIN)")
public class AdminDashboardController {

    private final AdminStatsService adminStatsService;
    private final UserService userService;
    private final NotificationService notificationService;

    /** Every figure is a live database aggregate - see {@code AdminStatsService}. */
    @GetMapping("/stats")
    @Operation(summary = "Sales, order, customer and inventory statistics")
    public ResponseEntity<AdminStatsResponse> stats() {
        return ResponseEntity.ok(adminStatsService.getDashboardStats());
    }

    @GetMapping("/users")
    @Operation(summary = "List registered users, with optional search")
    public ResponseEntity<PagedResponse<UserResponse>> users(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(userService.listUsers(q, page, size));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Fetch one user")
    public ResponseEntity<UserResponse> user(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserForAdmin(id));
    }

    /**
     * Enables or disables an account. Disabling takes effect on the user's very next
     * request, because the JWT filter re-checks the flag against the database.
     */
    @PutMapping("/users/{id}/enabled")
    @Operation(summary = "Enable or disable a user account")
    public ResponseEntity<UserResponse> setUserEnabled(@AuthenticationPrincipal UserPrincipal principal,
                                                       @PathVariable Long id,
                                                       @RequestParam boolean enabled) {
        return ResponseEntity.ok(userService.setEnabled(principal.getId(), id, enabled));
    }

    @GetMapping("/notifications")
    @Operation(summary = "List WhatsApp notification attempts (sent, failed and skipped)")
    public ResponseEntity<PagedResponse<NotificationResponse>> notifications(
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationService.list(status, page, size));
    }

    @GetMapping("/notifications/summary")
    @Operation(summary = "Notification counts per outcome")
    public ResponseEntity<Map<String, Long>> notificationSummary() {
        return ResponseEntity.ok(notificationService.countsByStatus());
    }
}
