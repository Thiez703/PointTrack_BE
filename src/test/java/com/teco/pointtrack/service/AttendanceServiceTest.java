package com.teco.pointtrack.service;

import com.teco.pointtrack.entity.Customer;
import com.teco.pointtrack.entity.SalaryLevel;
import com.teco.pointtrack.entity.AttendanceRecord;
import com.teco.pointtrack.entity.User;
import com.teco.pointtrack.entity.WorkSchedule;
import com.teco.pointtrack.entity.SystemSetting;
import com.teco.pointtrack.dto.attendance.CheckInResponse;
import com.teco.pointtrack.entity.enums.AttendanceStatus;
import com.teco.pointtrack.dto.attendance.AdminUpdateAttendanceRequest;
import com.teco.pointtrack.dto.attendance.CheckOutResponse;
import com.teco.pointtrack.entity.enums.WorkScheduleStatus;
import com.teco.pointtrack.exception.BadRequestException;
import com.teco.pointtrack.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AttendanceServiceTest {

    @Mock
    private WorkScheduleRepository workScheduleRepo;

    @Mock
    private AttendanceRecordRepository attendanceRecordRepo;

    @Mock
    private AttendancePhotoRepository attendancePhotoRepo;

    @Mock
    private ExplanationRequestRepository explanationRepo;

    @Mock
    private AttendanceAuditLogRepository auditLogRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private CustomerRepository customerRepo;

    @Mock
    private SystemSettingRepository systemSettingRepo;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private AttendanceService attendanceService;

    private Long userId = 1L;
    private Long workScheduleId = 100L;
    private MultipartFile photo;

    @BeforeEach
    void setUp() {
        photo = mock(MultipartFile.class);
        lenient().when(photo.isEmpty()).thenReturn(false);
    }

    @Test
    void checkIn_WhenScheduleIsForTomorrow_ShouldThrowBadRequest() {
        // Given: Server's today is April 6
        LocalDate serverToday = LocalDate.now();
        // Schedule is for tomorrow (relative to server)
        LocalDate scheduleDate = serverToday.plusDays(1);

        User user = User.builder().id(userId).build();
        WorkSchedule schedule = WorkSchedule.builder()
                .id(workScheduleId)
                .user(user)
                .workDate(scheduleDate)
                .status(WorkScheduleStatus.SCHEDULED)
                .build();

        when(workScheduleRepo.findByIdAndDeletedAtIsNull(workScheduleId)).thenReturn(Optional.of(schedule));

        // When/Then
        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            attendanceService.checkIn(workScheduleId, 10.0, 106.0, LocalDateTime.now(), "Note", photo, userId);
        });

        assertTrue(exception.getMessage() != null && !exception.getMessage().isBlank());
    }

    @Test
    void checkOut_ShouldCalculateSalaryFromWorkedMinutesAndLevelRate() {
        Long attendanceRecordId = 200L;
        BigDecimal baseRatePerHour = new BigDecimal("100000");
        BigDecimal otMultiplier = new BigDecimal("1.5");

        SalaryLevel salaryLevel = SalaryLevel.builder()
                .name("Cấp 3")
                .baseSalary(baseRatePerHour)
                .build();

        User user = User.builder()
                .id(userId)
                .salaryLevel(salaryLevel)
                .build();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime checkInTime = now.minusMinutes(95);

        Customer customer = Customer.builder()
                .id(1L)
                .name("Khách hàng A")
                .latitude(10.0)
                .longitude(106.0)
                .build();

        WorkSchedule schedule = WorkSchedule.builder()
                .id(11L)
                .user(user)
                .customer(customer)
                .workDate(now.toLocalDate())
                .scheduledStart(now.minusMinutes(120))
                .scheduledEnd(now.plusMinutes(60))
                .status(WorkScheduleStatus.IN_PROGRESS)
                .build();

        AttendanceRecord record = AttendanceRecord.builder()
                .id(attendanceRecordId)
                .workSchedule(schedule)
                .user(user)
                .checkInTime(checkInTime)
                .otMultiplier(otMultiplier)
                .build();

        when(attendanceRecordRepo.findById(attendanceRecordId)).thenReturn(Optional.of(record));
        when(systemSettingRepo.findById("GPS_RADIUS_METERS")).thenReturn(Optional.empty());
        when(systemSettingRepo.findById("LATE_CHECKOUT_THRESHOLD_MINUTES")).thenReturn(Optional.empty());
        when(fileStorageService.storeAttendancePhoto(photo)).thenReturn("checkout.jpg");
        when(photo.getSize()).thenReturn(1234L);
        when(photo.getContentType()).thenReturn("image/jpeg");
        when(photo.getOriginalFilename()).thenReturn("checkout.jpg");

        CheckOutResponse response = attendanceService.checkOut(
                attendanceRecordId,
                10.0,
                106.0,
                now,
                null,
                photo,
                userId
        );

        assertNotNull(response.getWorkedMinutes());
        assertEquals(response.getWorkedMinutes().intValue(), response.getActualMinutes());

        BigDecimal expectedSalary = BigDecimal.valueOf(response.getWorkedMinutes())
                .multiply(baseRatePerHour)
                .multiply(otMultiplier)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        assertEquals(expectedSalary, response.getEstimatedSalary());
        assertEquals(expectedSalary, record.getEstimatedSalary());
        assertEquals(response.getWorkedMinutes(), record.getWorkedMinutes());
        assertEquals(response.getWorkedHours(), record.getWorkedHours());
    }

    @Test
    void checkIn_WhenLateWithin15Minutes_ShouldStillBeOnTime() {
        LocalDateTime now = LocalDateTime.now();

        User user = User.builder().id(userId).build();
        Customer customer = Customer.builder()
                .id(1L)
                .name("Khách hàng A")
                .latitude(10.0)
                .longitude(106.0)
                .build();

        WorkSchedule schedule = WorkSchedule.builder()
                .id(workScheduleId)
                .user(user)
                .customer(customer)
                .workDate(now.toLocalDate())
                .startTime(now.toLocalTime().minusMinutes(10))
                .endTime(now.toLocalTime().plusHours(1))
                .scheduledStart(now.minusMinutes(10))
                .scheduledEnd(now.plusHours(1))
                .status(WorkScheduleStatus.SCHEDULED)
                .build();

        when(workScheduleRepo.findByIdAndDeletedAtIsNull(workScheduleId)).thenReturn(Optional.of(schedule));
        when(attendanceRecordRepo.findByWorkScheduleId(workScheduleId)).thenReturn(Optional.empty());
        when(userRepo.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(systemSettingRepo.findById("GRACE_PERIOD_MINUTES")).thenReturn(
                Optional.of(SystemSetting.builder().key("GRACE_PERIOD_MINUTES").value("15").build())
        );
        when(systemSettingRepo.findById("GPS_RADIUS_METERS")).thenReturn(Optional.empty());
        when(fileStorageService.storeAttendancePhoto(photo)).thenReturn("checkin.jpg");
        when(photo.getSize()).thenReturn(1234L);
        when(photo.getContentType()).thenReturn("image/jpeg");
        when(photo.getOriginalFilename()).thenReturn("checkin.jpg");

        CheckInResponse response = attendanceService.checkIn(
                workScheduleId,
                10.0,
                106.0,
                now,
                "Đến trễ nhẹ",
                photo,
                userId
        );

        assertEquals(AttendanceStatus.ON_TIME, response.getStatus());
        assertEquals(0, response.getLateMinutes());
                verifyNoInteractions(explanationRepo);
    }

    @Test
    void adminUpdateAttendance_ShouldRecalculateWorkedHoursAndSalary() {
        Long recordId = 300L;
        Long adminId = 999L;

        SalaryLevel salaryLevel = SalaryLevel.builder()
                .name("Cấp 2")
                .baseSalary(new BigDecimal("70000"))
                .build();

        User user = User.builder()
                .id(userId)
                .salaryLevel(salaryLevel)
                .build();

        AttendanceRecord record = AttendanceRecord.builder()
                .id(recordId)
                .user(user)
                .checkInTime(LocalDateTime.of(2026, 4, 8, 8, 0))
                .checkOutTime(LocalDateTime.of(2026, 4, 8, 12, 0))
                .actualMinutes(240)
                .workedMinutes(240L)
                .workedHours(4.0)
                .estimatedSalary(new BigDecimal("280000.00"))
                .build();

        AdminUpdateAttendanceRequest req = new AdminUpdateAttendanceRequest();
        req.setCheckOutTime(LocalDateTime.of(2026, 4, 8, 13, 30));
        req.setReason("Điều chỉnh theo camera thực tế");

        when(attendanceRecordRepo.findById(recordId)).thenReturn(Optional.of(record));

        attendanceService.adminUpdateAttendance(recordId, req, adminId);

        assertEquals(Integer.valueOf(330), record.getActualMinutes());
        assertEquals(Long.valueOf(330), record.getWorkedMinutes());
        assertEquals(5.5, record.getWorkedHours());
        assertEquals(new BigDecimal("385000.00"), record.getEstimatedSalary());
    }
}
