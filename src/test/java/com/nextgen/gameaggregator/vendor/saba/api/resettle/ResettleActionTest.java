package com.nextgen.gameaggregator.vendor.saba.api.resettle;

import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.RawBatchProcessIdempotentLogService;
import com.nextgen.gameaggregator.sport.service.SportWalletServiceImpl;
import com.nextgen.gameaggregator.vendor.saba.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.saba.service.VendorService;
import com.nextgen.gameaggregator.vendor.saba.vo.GeneralVo;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResettleActionTest {

    @Mock
    private HttpService httpService;

    @Mock
    private RawBatchProcessIdempotentLogService rawBatchProcessIdempotentLogService;

    @Mock
    private SportWalletServiceImpl sportWalletService;

    @Mock
    private VendorService vendorService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private ResettleAction resettleAction;

    private HttpRequestLog httpRequestLog;

    @BeforeEach
    void setUp() {
        httpRequestLog = new HttpRequestLog();
        httpRequestLog.setId("trace-12345");
        httpRequestLog.setUrl("https://merchant.domain/resettle");

        when(httpService.start(httpServletRequest)).thenReturn(httpRequestLog);
    }

    @Test
    @DisplayName("When isOnlyWinlostDateChanged is true, should skip sportWalletService.resettle()")
    void action_WhenOnlyWinLostDateChangedIsTrue_ShouldSkipResettle() throws Exception {
        // Arrange: SABA JSON payload matching vendor spec with isOnlyWinlostDateChanged: true
        String jsonPayload = """
                {
                  "key": "test_key",
                  "message": {
                    "action": "Resettle",
                    "operationId": "4200500_1_25",
                    "txns": [
                      {
                        "userId": "player01",
                        "refId": "4200500_555",
                        "txId": 339482738748,
                        "updateTime": "2021-09-08T04:49:32.383-04:00",
                        "winlostDate": "2021-09-08T00:00:00.000-04:00",
                        "status": "won",
                        "payout": 3.5600,
                        "creditAmount": 3.5600,
                        "debitAmount": 0.0,
                        "extraStatus": "",
                        "settlementTime": "2021-09-08T04:49:26.3",
                        "extraInfo": {
                          "isOnlyWinlostDateChanged": true
                        }
                      }
                    ]
                  }
                }
                """;

        httpRequestLog.setRequestBody(jsonPayload);

        when(vendorService.generateBatchProcessId(anyString(), anyString())).thenReturn("batch_4200500_1_25");
        when(rawBatchProcessIdempotentLogService.checkExists("batch_4200500_1_25")).thenReturn(null);

        // Act
        GeneralVo response = resettleAction.action(httpServletRequest);

        // Assertions
        assertNotNull(response);
        assertEquals(ResponseCode.SUCCESS.status, response.getStatus());
        assertFalse(response.hasError());

        // Key assertion: ensure wallet service resettle is NOT invoked
        verify(sportWalletService, never()).resettle(anyString(), any(ResettleTransactionDto.class), any(HttpRequestLog.class));

        // Verify batch log created
        verify(rawBatchProcessIdempotentLogService).create(any());
    }

    @Test
    @DisplayName("When isOnlyWinlostDateChanged is false, should call sportWalletService.resettle()")
    void action_WhenOnlyWinLostDateChangedIsFalse_ShouldProcessResettle() throws Exception {
        String jsonPayload = """
                {
                  "key": "test_key",
                  "message": {
                    "action": "Resettle",
                    "operationId": "4200500_1_25",
                    "txns": [
                      {
                        "userId": "player01",
                        "refId": "4200500_555",
                        "txId": 339482738748,
                        "updateTime": "2021-09-08T04:49:32.383-04:00",
                        "winlostDate": "2021-09-08T00:00:00.000-04:00",
                        "status": "won",
                        "payout": 3.5600,
                        "creditAmount": 3.5600,
                        "debitAmount": 0.0,
                        "extraStatus": "",
                        "settlementTime": "2021-09-08T04:49:26.3",
                        "extraInfo": {
                          "isOnlyWinlostDateChanged": false
                        }
                      }
                    ]
                  }
                }
                """;

        httpRequestLog.setRequestBody(jsonPayload);

        when(vendorService.generateBatchProcessId(anyString(), anyString())).thenReturn("batch_4200500_1_25");
        when(rawBatchProcessIdempotentLogService.checkExists("batch_4200500_1_25")).thenReturn(null);

        // Act
        GeneralVo response = resettleAction.action(httpServletRequest);

        // Assertions
        assertNotNull(response);
        assertEquals(ResponseCode.SUCCESS.status, response.getStatus());
        assertFalse(response.hasError());

        // Assert resettle WAS executed exactly once
        verify(sportWalletService).resettle(eq("trace-12345"), any(ResettleTransactionDto.class), eq(httpRequestLog));
    }

    @Test
    @DisplayName("When extraInfo is null (backward compatibility), should call sportWalletService.resettle()")
    void action_WhenExtraInfoIsNull_ShouldProcessResettle() throws Exception {
        // Legacy payload omitting extraInfo completely
        String jsonPayload = """
                {
                  "key": "test_key",
                  "message": {
                    "action": "Resettle",
                    "operationId": "4200500_1_25",
                    "txns": [
                      {
                        "userId": "player01",
                        "refId": "4200500_555",
                        "txId": 339482738748,
                        "updateTime": "2021-09-08T04:49:32.383-04:00",
                        "winlostDate": "2021-09-08T00:00:00.000-04:00",
                        "status": "won",
                        "payout": 3.5600,
                        "creditAmount": 3.5600,
                        "debitAmount": 0.0,
                        "extraStatus": "",
                        "settlementTime": "2021-09-08T04:49:26.3"
                      }
                    ]
                  }
                }
                """;

        httpRequestLog.setRequestBody(jsonPayload);

        when(vendorService.generateBatchProcessId(anyString(), anyString())).thenReturn("batch_4200500_1_25");
        when(rawBatchProcessIdempotentLogService.checkExists("batch_4200500_1_25")).thenReturn(null);

        // Act
        GeneralVo response = resettleAction.action(httpServletRequest);

        // Assertions
        assertNotNull(response);
        assertEquals(ResponseCode.SUCCESS.status, response.getStatus());
        assertFalse(response.hasError());

        // Assert resettle WAS executed for legacy payloads without extraInfo
        verify(sportWalletService).resettle(eq("trace-12345"), any(ResettleTransactionDto.class), eq(httpRequestLog));
    }
}