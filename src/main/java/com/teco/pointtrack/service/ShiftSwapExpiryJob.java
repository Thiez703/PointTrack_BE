package com.teco.pointtrack.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job: Tự động hủy yêu cầu đổi ca hết hạn phản hồi (PENDING_EMPLOYEE).
 * Chạy mỗi 5 phút.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ShiftSwapExpiryJob {

    private final ShiftSwapService shiftSwapService;

    @Scheduled(fixedDelay = 300_000) // 5 phút
    public void expirePendingRequests() {
        log.debug("ShiftSwapExpiryJob: checking expired requests...");
        shiftSwapService.autoExpireRequests();
    }
}
