package com.ecommerce.service;

import com.ecommerce.dto.admin.NotificationResponse;
import com.ecommerce.dto.common.PagedResponse;
import com.ecommerce.entity.NotificationStatus;
import com.ecommerce.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Read access to the notification audit trail.
 *
 * <p>Writing rows is the job of {@code OrderNotificationListener}; this service only
 * exposes them to the admin dashboard so delivery problems are visible rather than
 * buried in server logs.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> list(NotificationStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));

        var results = (status == null)
                ? notificationRepository.findAllByOrderByCreatedAtDesc(pageable)
                : notificationRepository.findByStatusOrderByCreatedAtDesc(status, pageable);

        return PagedResponse.from(results, NotificationResponse::from);
    }

    /** Delivery attempts for one order, shown on the admin order detail screen. */
    @Transactional(readOnly = true)
    public List<NotificationResponse> listForOrder(Long orderId) {
        return notificationRepository.findByOrderIdOrderByCreatedAtDesc(orderId)
                .stream().map(NotificationResponse::from).toList();
    }

    /** Counts per outcome, so the dashboard can surface a failing WhatsApp integration. */
    @Transactional(readOnly = true)
    public Map<String, Long> countsByStatus() {
        return Map.of(
                NotificationStatus.SENT.name(), notificationRepository.countByStatus(NotificationStatus.SENT),
                NotificationStatus.FAILED.name(), notificationRepository.countByStatus(NotificationStatus.FAILED),
                NotificationStatus.SKIPPED.name(), notificationRepository.countByStatus(NotificationStatus.SKIPPED)
        );
    }
}
