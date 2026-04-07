package com.teco.pointtrack.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job tự động xử lý trạng thái ca làm việc (MISSED, MISSING_OUT).
 * Chạy mỗi 30 phút.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AttendanceAutoJob {

    private final ShiftService shiftService;

    @Scheduled(fixedDelay = 1800_000) // 30 phút
    public void processShifts() {
        log.debug("AttendanceAutoJob: checking missed and missing-out shifts...");
        shiftService.autoProcessShifts();
    }
}
