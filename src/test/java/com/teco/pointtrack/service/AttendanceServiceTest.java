package com.teco.pointtrack.service;

import com.teco.pointtrack.entity.Customer;
import com.teco.pointtrack.entity.User;
import com.teco.pointtrack.entity.WorkSchedule;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
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
        when(photo.isEmpty()).thenReturn(false);
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

        assertTrue(exception.getMessage().contains("Chỉ được check-in vào ngày làm việc"));
    }
}
