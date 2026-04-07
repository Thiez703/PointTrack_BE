package com.teco.pointtrack.service;

import com.teco.pointtrack.dto.shiftswap.RespondSwapRequest;
import com.teco.pointtrack.entity.Customer;
import com.teco.pointtrack.entity.Shift;
import com.teco.pointtrack.entity.ShiftSwapRequest;
import com.teco.pointtrack.entity.User;
import com.teco.pointtrack.entity.enums.SwapStatus;
import com.teco.pointtrack.entity.enums.SwapType;
import com.teco.pointtrack.exception.BadRequestException;
import com.teco.pointtrack.exception.Forbidden;
import com.teco.pointtrack.repository.ShiftRepository;
import com.teco.pointtrack.repository.ShiftSwapRepository;
import com.teco.pointtrack.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShiftSwapServiceTest {

    @Mock ShiftSwapRepository swapRepository;
    @Mock ShiftRepository     shiftRepository;
    @Mock UserRepository      userRepository;

    @InjectMocks ShiftSwapService service;

    User userA, userB;
    Shift shiftA, shiftB;
    Customer customer;

    @BeforeEach
    void setUp() {
        customer = Customer.builder().build();

        userA = User.builder().build();
        userA.setId(1L);
        userA.setFullName("Nguyễn A");

        userB = User.builder().build();
        userB.setId(2L);
        userB.setFullName("Trần B");

        shiftA = Shift.builder()
                .shiftDate(LocalDate.now().plusDays(1))
                .customer(customer)
                .build();
        shiftA.setId(10L);
        shiftA.setEmployee(userA);

        shiftB = Shift.builder()
                .shiftDate(LocalDate.now().plusDays(1))
                .customer(customer)
                .build();
        shiftB.setId(20L);
        shiftB.setEmployee(userB);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // executeSwap: SWAP
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void executeSwap_SWAP_swapsEmployees() {
        ShiftSwapRequest swap = buildSwap(SwapType.SWAP, SwapStatus.PENDING_EMPLOYEE);
        when(shiftRepository.saveAll(anyList())).thenReturn(List.of(shiftA, shiftB));

        service.executeSwap(swap);

        // Sau swap: shiftA có employee B, shiftB có employee A
        assertThat(shiftA.getEmployee()).isEqualTo(userB);
        assertThat(shiftB.getEmployee()).isEqualTo(userA);
        verify(shiftRepository).saveAll(anyList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // executeSwap: TRANSFER
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void executeSwap_TRANSFER_assignsReceiverToShiftA() {
        ShiftSwapRequest swap = buildSwap(SwapType.TRANSFER, SwapStatus.PENDING_EMPLOYEE);

        service.executeSwap(swap);

        // BaseEntity.equals() dùng các audit field (đều null) nên shiftA.equals(shiftB).
        // Dùng ArgumentCaptor để verify bằng reference thay vì equals.
        assertThat(shiftA.getEmployee()).isSameAs(userB);
        ArgumentCaptor<Shift> captor = ArgumentCaptor.forClass(Shift.class);
        verify(shiftRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(shiftA);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // executeSwap: SAME_DAY
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void executeSwap_SAME_DAY_freesOldShiftAndAssignsNew() {
        Shift newShift = Shift.builder().shiftDate(LocalDate.now().plusDays(1)).customer(customer).build();
        newShift.setId(30L);

        ShiftSwapRequest swap = ShiftSwapRequest.builder()
                .type(SwapType.SAME_DAY)
                .status(SwapStatus.PENDING_ADMIN)
                .requester(userA)
                .requesterShift(shiftA)
                .targetShift(newShift)
                .reason("lý do test")
                .build();

        service.executeSwap(swap);

        assertThat(shiftA.getEmployee()).isNull();
        assertThat(newShift.getEmployee()).isSameAs(userA);
        // Dùng ArgumentCaptor để phân biệt shiftA và newShift (BaseEntity.equals dùng audit fields = null)
        ArgumentCaptor<Shift> captor = ArgumentCaptor.forClass(Shift.class);
        verify(shiftRepository, times(2)).save(captor.capture());
        List<Shift> saved = captor.getAllValues();
        assertThat(saved).anySatisfy(s -> assertThat(s).isSameAs(shiftA));
        assertThat(saved).anySatisfy(s -> assertThat(s).isSameAs(newShift));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // executeSwap: OTHER_DAY
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void executeSwap_OTHER_DAY_freesOldShiftAndAssignsNew() {
        Shift futureShift = Shift.builder()
                .shiftDate(LocalDate.now().plusDays(3))
                .customer(customer)
                .build();
        futureShift.setId(40L);

        ShiftSwapRequest swap = ShiftSwapRequest.builder()
                .type(SwapType.OTHER_DAY)
                .status(SwapStatus.PENDING_ADMIN)
                .requester(userA)
                .requesterShift(shiftA)
                .targetShift(futureShift)
                .targetDate(LocalDate.now().plusDays(3))
                .reason("lý do test")
                .build();

        service.executeSwap(swap);

        assertThat(shiftA.getEmployee()).isNull();
        assertThat(futureShift.getEmployee()).isSameAs(userA);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // respond: không phải receiver → Forbidden
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void respond_notReceiver_throwsForbidden() {
        ShiftSwapRequest swap = buildSwap(SwapType.SWAP, SwapStatus.PENDING_EMPLOYEE);
        when(swapRepository.findById(1L)).thenReturn(Optional.of(swap));

        RespondSwapRequest req = new RespondSwapRequest(true, null);

        // User C (id=99) không phải receiver
        assertThrows(Forbidden.class, () -> service.respond(1L, 99L, req));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // respond: hết hạn → BadRequestException
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void respond_expired_throwsBadRequest() {
        ShiftSwapRequest swap = buildSwap(SwapType.SWAP, SwapStatus.PENDING_EMPLOYEE);
        swap.setExpiredAt(LocalDateTime.now().minusHours(1)); // đã hết hạn
        when(swapRepository.findById(1L)).thenReturn(Optional.of(swap));

        RespondSwapRequest req = new RespondSwapRequest(true, null);

        assertThrows(BadRequestException.class, () -> service.respond(1L, 2L, req));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // respond: từ chối không có lý do → BadRequestException
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void respond_rejectWithoutReason_throwsBadRequest() {
        ShiftSwapRequest swap = buildSwap(SwapType.SWAP, SwapStatus.PENDING_EMPLOYEE);
        swap.setExpiredAt(LocalDateTime.now().plusHours(10));
        when(swapRepository.findById(1L)).thenReturn(Optional.of(swap));
        when(userRepository.findById(2L)).thenReturn(Optional.of(userB));

        RespondSwapRequest req = new RespondSwapRequest(false, "  "); // lý do trống

        assertThrows(BadRequestException.class, () -> service.respond(1L, 2L, req));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // cancel: không phải requester → Forbidden
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void cancel_notRequester_throwsForbidden() {
        ShiftSwapRequest swap = buildSwap(SwapType.SWAP, SwapStatus.PENDING_EMPLOYEE);
        when(swapRepository.findById(1L)).thenReturn(Optional.of(swap));

        assertThrows(Forbidden.class, () -> service.cancel(1L, 99L));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private ShiftSwapRequest buildSwap(SwapType type, SwapStatus status) {
        ShiftSwapRequest swap = ShiftSwapRequest.builder()
                .type(type)
                .status(status)
                .requester(userA)
                .requesterShift(shiftA)
                .receiver(userB)
                .receiverShift(shiftB)
                .reason("lý do test")
                .expiredAt(LocalDateTime.now().plusHours(24))
                .build();
        swap.setId(1L);
        return swap;
    }
}
