package com.nextgen.gameaggregator.vendor.jili.api.freespin;

import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.vendor.jili.api.bet.BetDto;
import com.nextgen.gameaggregator.vendor.jili.api.bet.BetVo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JiliFreeSpinPayoutProcessorTest {

    @InjectMocks
    private JiliFreeSpinPayoutProcessor processor;

    @Mock
    private JiliFreeSpinPayoutHandler handler;

    private BetDto buildBetDto(BigDecimal winloseAmount) {
        BetDto dto = new BetDto();
        dto.setReqId("abc-001");
        dto.setToken("tok-abc");
        dto.setCurrency("THB");
        dto.setGame(110);
        dto.setRound(new BigInteger("17238050501001102001"));
        dto.setWagersTime(new BigInteger("1592559162"));
        dto.setBetAmount(BigDecimal.ZERO);
        dto.setWinloseAmount(winloseAmount);
        FreeSpinData fsd = new FreeSpinData();
        fsd.setReferenceId("FS001");
        fsd.setRemain(9);
        fsd.setDeduct(1);
        dto.setFreeSpinData(fsd);
        return dto;
    }

    @Nested
    @DisplayName("N3 guard — negative winloseAmount")
    class N3Guard {

        @Test
        @DisplayName("negative winloseAmount throws InvalidRequestException before handler is called")
        void negativeWinloseAmount_throwsInvalidRequestException() {
            BetDto dto = buildBetDto(new BigDecimal("-1.00"));

            assertThrows(InvalidRequestException.class,
                    () -> processor.process(dto, "player001", "THB", "tok-abc"));

            verifyNoInteractions(handler);
        }
    }

    @Nested
    @DisplayName("Happy path — correct request fields forwarded to handler")
    class HappyPath {

        @Test
        @DisplayName("1.1: all request fields are forwarded to JiliFreeSpinPayoutRequest correctly")
        void happyPath_fieldsForwardedToHandler() throws Exception {
            BetDto dto = buildBetDto(new BigDecimal("55.00"));
            BetVo expected = new BetVo();
            expected.setBalance(new BigDecimal("1055.00"));
            when(handler.process(any(JiliFreeSpinPayoutRequest.class))).thenReturn(expected);

            BetVo result = processor.process(dto, "player001", "THB", "tok-abc");

            assertSame(expected, result);

            ArgumentCaptor<JiliFreeSpinPayoutRequest> captor =
                    ArgumentCaptor.forClass(JiliFreeSpinPayoutRequest.class);
            verify(handler).process(captor.capture());
            JiliFreeSpinPayoutRequest captured = captor.getValue();

            assertEquals("player001", captured.getVendorPlayerUsername());
            assertEquals("THB", captured.getVendorCurrencyCode());
            assertEquals("tok-abc", captured.getToken());
            assertEquals("abc-001", captured.getReqId());
            assertEquals("17238050501001102001", captured.getRound());
            assertEquals(new BigDecimal("55.00"), captured.getWinloseAmount());
            assertEquals(new BigInteger("1592559162"), captured.getWagersTime());
            assertEquals("FS001", captured.getFreeSpinData().getReferenceId());
        }

        @Test
        @DisplayName("zero winloseAmount is accepted (not negative)")
        void zeroWinloseAmount_isAccepted() throws Exception {
            BetDto dto = buildBetDto(BigDecimal.ZERO);
            when(handler.process(any())).thenReturn(new BetVo());

            assertDoesNotThrow(() -> processor.process(dto, "player001", "THB", "tok-abc"));
            verify(handler).process(any(JiliFreeSpinPayoutRequest.class));
        }
    }
}
