package com.teco.pointtrack.service;

import com.teco.pointtrack.dto.shiftswap.*;
import com.teco.pointtrack.entity.Shift;
import com.teco.pointtrack.entity.ShiftSwapRequest;
import com.teco.pointtrack.entity.User;
import com.teco.pointtrack.entity.enums.SwapStatus;
import com.teco.pointtrack.entity.enums.SwapType;
import com.teco.pointtrack.exception.BadRequestException;
import com.teco.pointtrack.exception.Forbidden;
import com.teco.pointtrack.exception.NotFoundException;
import com.teco.pointtrack.repository.ShiftRepository;
import com.teco.pointtrack.repository.ShiftSwapRepository;
import com.teco.pointtrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShiftSwapService {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ShiftSwapRepository swapRepository;
    private final ShiftRepository     shiftRepository;
    private final UserRepository      userRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // GET /v1/shift-swap — Danh sách yêu cầu
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ShiftSwapResponse> getRequests(
            Long currentUserId,
            boolean isAdmin,
            String tab,
            SwapStatus status,
            int page,
            int size) {

        Pageable pageable = PageRequest.of(page, size);

        if (isAdmin && (tab == null || tab.isBlank())) {
            return swapRepository.findAll(status, pageable).map(this::toResponse);
        }

        if ("received".equalsIgnoreCase(tab)) {
            return swapRepository.findByReceiver(currentUserId, status, pageable).map(this::toResponse);
        }

        // default: sent
        return swapRepository.findByRequester(currentUserId, status, pageable).map(this::toResponse);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /v1/shift-swap/:id
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ShiftSwapResponse getById(Long swapId, Long currentUserId, boolean isAdmin) {
        ShiftSwapRequest swap = findById(swapId);
        if (!isAdmin
                && !swap.getRequester().getId().equals(currentUserId)
                && (swap.getReceiver() == null || !swap.getReceiver().getId().equals(currentUserId))) {
            throw new Forbidden();
        }
        return toResponse(swap);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /v1/shift-swap — Tạo yêu cầu
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public ShiftSwapResponse createRequest(Long requesterId, CreateShiftSwapRequest req) {
        User requester = findUser(requesterId);
        Shift myShift  = findShiftBelongsTo(req.myShiftId(), requesterId);

        validateShiftNotStarted(myShift);
        validateNoPendingSwap(req.myShiftId());

        ShiftSwapRequest swap = ShiftSwapRequest.builder()
                .type(req.type())
                .requester(requester)
                .requesterShift(myShift)
                .reason(req.reason())
                .build();

        switch (req.type()) {
            case SWAP -> {
                requireField(req.targetEmployeeId(), "targetEmployeeId");
                requireField(req.targetShiftId(),    "targetShiftId");

                if (req.targetEmployeeId().equals(requesterId)) {
                    throw new BadRequestException("SWAP_SELF");
                }

                User receiver      = findUser(req.targetEmployeeId());
                Shift receiverShift = findShiftBelongsTo(req.targetShiftId(), req.targetEmployeeId());

                swap.setReceiver(receiver);
                swap.setReceiverShift(receiverShift);
                swap.setStatus(SwapStatus.PENDING_EMPLOYEE);
                swap.setExpiredAt(LocalDateTime.now(VN_ZONE).plusHours(24));
            }

            case TRANSFER -> {
                requireField(req.targetEmployeeId(), "targetEmployeeId");
                if (req.targetEmployeeId().equals(requesterId)) {
                    throw new BadRequestException("SWAP_SELF");
                }

                User receiver = findUser(req.targetEmployeeId());
                swap.setReceiver(receiver);
                swap.setStatus(SwapStatus.PENDING_EMPLOYEE);
                swap.setExpiredAt(LocalDateTime.now(VN_ZONE).plusHours(24));
            }

            case SAME_DAY -> {
                if (req.targetShiftId() != null) {
                    Shift targetShift = shiftRepository.findById(req.targetShiftId())
                            .orElseThrow(() -> new NotFoundException("SHIFT_NOT_FOUND"));
                    swap.setTargetShift(targetShift);
                }
                swap.setStatus(SwapStatus.PENDING_ADMIN);
            }

            case OTHER_DAY -> {
                requireField(req.targetDate(), "targetDate");
                if (req.targetDate().isBefore(LocalDate.now(VN_ZONE))) {
                    throw new BadRequestException("TARGET_DATE_MUST_BE_FUTURE");
                }
                swap.setTargetDate(req.targetDate());
                if (req.targetShiftId() != null) {
                    Shift targetShift = shiftRepository.findById(req.targetShiftId())
                            .orElseThrow(() -> new NotFoundException("SHIFT_NOT_FOUND"));
                    swap.setTargetShift(targetShift);
                }
                swap.setStatus(SwapStatus.PENDING_ADMIN);
            }
        }

        ShiftSwapRequest saved = swapRepository.save(swap);
        log.info("ShiftSwap created: id={} type={} requester={} status={}",
                saved.getId(), saved.getType(), requesterId, saved.getStatus());
        return toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PATCH /v1/shift-swap/:id/respond — NV_B chấp nhận / từ chối
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public ShiftSwapResponse respond(Long swapId, Long currentUserId, RespondSwapRequest req) {
        ShiftSwapRequest swap = findById(swapId);

        if (swap.getReceiver() == null || !swap.getReceiver().getId().equals(currentUserId)) {
            throw new Forbidden();
        }
        if (swap.getStatus() != SwapStatus.PENDING_EMPLOYEE) {
            throw new BadRequestException("SWAP_CANNOT_RESPOND");
        }
        if (swap.getExpiredAt() != null
                && LocalDateTime.now(VN_ZONE).isAfter(swap.getExpiredAt())) {
            throw new BadRequestException("SWAP_EXPIRED");
        }

        User reviewer = findUser(currentUserId);

        if (req.accept()) {
            executeSwap(swap);
            swap.setStatus(SwapStatus.APPROVED);
        } else {
            if (req.rejectReason() == null || req.rejectReason().isBlank()) {
                throw new BadRequestException("REJECT_REASON_REQUIRED");
            }
            swap.setStatus(SwapStatus.REJECTED);
            swap.setRejectReason(req.rejectReason());
        }

        swap.setReviewedBy(reviewer);
        swap.setReviewedAt(LocalDateTime.now(VN_ZONE));

        return toResponse(swapRepository.save(swap));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PATCH /v1/shift-swap/:id/approve — Admin duyệt
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public ShiftSwapResponse approve(Long swapId, Long adminId) {
        ShiftSwapRequest swap = findById(swapId);

        if (swap.getStatus() != SwapStatus.PENDING_ADMIN) {
            throw new BadRequestException("SWAP_CANNOT_APPROVE");
        }

        executeSwap(swap);
        swap.setStatus(SwapStatus.APPROVED);
        swap.setReviewedBy(findUser(adminId));
        swap.setReviewedAt(LocalDateTime.now(VN_ZONE));

        return toResponse(swapRepository.save(swap));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PATCH /v1/shift-swap/:id/reject — Admin từ chối
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public ShiftSwapResponse reject(Long swapId, Long adminId, RejectSwapRequest req) {
        ShiftSwapRequest swap = findById(swapId);

        if (swap.getStatus() != SwapStatus.PENDING_ADMIN) {
            throw new BadRequestException("SWAP_CANNOT_REJECT");
        }

        swap.setStatus(SwapStatus.REJECTED);
        swap.setRejectReason(req.rejectReason());
        swap.setReviewedBy(findUser(adminId));
        swap.setReviewedAt(LocalDateTime.now(VN_ZONE));

        return toResponse(swapRepository.save(swap));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /v1/shift-swap/:id — NV_A hủy yêu cầu
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void cancel(Long swapId, Long currentUserId) {
        ShiftSwapRequest swap = findById(swapId);

        if (!swap.getRequester().getId().equals(currentUserId)) {
            throw new Forbidden();
        }
        if (swap.getStatus() != SwapStatus.PENDING_EMPLOYEE
                && swap.getStatus() != SwapStatus.PENDING_ADMIN) {
            throw new BadRequestException("SWAP_CANNOT_CANCEL");
        }

        swap.setStatus(SwapStatus.CANCELLED);
        swapRepository.save(swap);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /v1/shift-swap/available-shifts
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ShiftSwapResponse.ShiftInfo> getAvailableShifts(
            LocalDate date, Long excludeShiftId) {

        return shiftRepository.findOpenShifts(date != null ? date : LocalDate.now(VN_ZONE))
                .stream()
                .filter(s -> excludeShiftId == null || !s.getId().equals(excludeShiftId))
                .filter(s -> date == null || s.getShiftDate().equals(date))
                .map(this::toShiftInfo)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /v1/shift-swap/available-employees
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ShiftSwapResponse.UserInfo> getAvailableEmployees(Long shiftId) {
        Shift myShift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new NotFoundException("SHIFT_NOT_FOUND"));

        // Trả NV có ca cùng ngày (để SWAP) — không phải chính mình
        List<Long> busyIds = shiftRepository.findBusyEmployeeIdsByDate(myShift.getShiftDate());
        busyIds = busyIds.stream()
                .filter(id -> myShift.getEmployee() == null || !id.equals(myShift.getEmployee().getId()))
                .toList();

        return userRepository.findAllById(busyIds).stream()
                .map(u -> ShiftSwapResponse.UserInfo.builder()
                        .id(u.getId())
                        .fullName(u.getFullName())
                        .avatarUrl(u.getAvatarUrl())
                        .build())
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scheduled: Tự động hủy yêu cầu hết hạn (gọi từ ShiftSwapExpiryJob)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void autoExpireRequests() {
        LocalDateTime now = LocalDateTime.now(VN_ZONE);
        List<ShiftSwapRequest> expired = swapRepository.findExpiredPending(now);

        if (expired.isEmpty()) return;

        expired.forEach(swap -> {
            swap.setStatus(SwapStatus.CANCELLED);
            swap.setRejectReason("Tự động hủy do hết hạn phản hồi");
        });
        swapRepository.saveAll(expired);
        log.info("Auto-expired {} shift swap request(s)", expired.size());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core logic: thực hiện đổi ca
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    protected void executeSwap(ShiftSwapRequest swap) {
        switch (swap.getType()) {

            case SWAP -> {
                // Hoán đổi nhân viên giữa 2 ca
                Shift shiftA = swap.getRequesterShift();
                Shift shiftB = swap.getReceiverShift();

                User tempEmployee = shiftA.getEmployee();
                shiftA.setEmployee(shiftB.getEmployee());
                shiftB.setEmployee(tempEmployee);

                shiftRepository.saveAll(List.of(shiftA, shiftB));
            }

            case TRANSFER -> {
                // A nhường ca cho B (B nhận, A mất ca)
                Shift shiftA = swap.getRequesterShift();
                shiftA.setEmployee(swap.getReceiver());
                shiftRepository.save(shiftA);
            }

            case SAME_DAY, OTHER_DAY -> {
                // A rời ca cũ, vào ca mới
                Shift oldShift = swap.getRequesterShift();
                oldShift.setEmployee(null);
                shiftRepository.save(oldShift);

                if (swap.getTargetShift() != null) {
                    Shift newShift = swap.getTargetShift();
                    newShift.setEmployee(swap.getRequester());
                    shiftRepository.save(newShift);
                }
            }
        }

        log.info("Swap executed: id={} type={}", swap.getId(), swap.getType());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private ShiftSwapRequest findById(Long id) {
        return swapRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("SHIFT_SWAP_NOT_FOUND"));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("EMPLOYEE_NOT_FOUND"));
    }

    private Shift findShiftBelongsTo(Long shiftId, Long employeeId) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new NotFoundException("SHIFT_NOT_FOUND"));
        if (shift.getEmployee() == null || !shift.getEmployee().getId().equals(employeeId)) {
            throw new BadRequestException("SHIFT_NOT_BELONG_TO_EMPLOYEE");
        }
        return shift;
    }

    private void validateShiftNotStarted(Shift shift) {
        // Không cho đổi ca đã qua hoặc đang IN_PROGRESS / COMPLETED
        LocalDate today = LocalDate.now(VN_ZONE);
        if (shift.getShiftDate().isBefore(today)) {
            throw new BadRequestException("SHIFT_ALREADY_PASSED");
        }
        if (shift.getCheckInTime() != null) {
            throw new BadRequestException("SHIFT_ALREADY_STARTED");
        }
    }

    private void validateNoPendingSwap(Long shiftId) {
        if (swapRepository.existsPendingForRequesterShift(shiftId)) {
            throw new BadRequestException("SHIFT_SWAP_ALREADY_PENDING");
        }
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException("FIELD_REQUIRED", fieldName);
        }
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private ShiftSwapResponse toResponse(ShiftSwapRequest swap) {
        return ShiftSwapResponse.builder()
                .id(swap.getId())
                .type(swap.getType())
                .status(swap.getStatus())
                .requester(toUserInfo(swap.getRequester()))
                .requesterShift(toShiftInfo(swap.getRequesterShift()))
                .receiver(swap.getReceiver() != null ? toUserInfo(swap.getReceiver()) : null)
                .receiverShift(swap.getReceiverShift() != null ? toShiftInfo(swap.getReceiverShift()) : null)
                .targetShift(swap.getTargetShift() != null ? toShiftInfo(swap.getTargetShift()) : null)
                .targetDate(swap.getTargetDate())
                .reason(swap.getReason())
                .rejectReason(swap.getRejectReason())
                .expiredAt(swap.getExpiredAt())
                .reviewedBy(swap.getReviewedBy() != null ? toUserInfo(swap.getReviewedBy()) : null)
                .reviewedAt(swap.getReviewedAt())
                .createdAt(swap.getCreatedAt())
                .updatedAt(swap.getUpdatedAt())
                .build();
    }

    private ShiftSwapResponse.UserInfo toUserInfo(User user) {
        if (user == null) return null;
        return ShiftSwapResponse.UserInfo.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    private ShiftSwapResponse.ShiftInfo toShiftInfo(Shift shift) {
        if (shift == null) return null;
        return ShiftSwapResponse.ShiftInfo.builder()
                .id(shift.getId())
                .shiftDate(shift.getShiftDate())
                .startTime(shift.getStartTime())
                .endTime(shift.getEndTime())
                .customerName(shift.getCustomer() != null ? shift.getCustomer().getName() : null)
                .customerAddress(shift.getCustomer() != null ? shift.getCustomer().getAddress() : null)
                .build();
    }
}
