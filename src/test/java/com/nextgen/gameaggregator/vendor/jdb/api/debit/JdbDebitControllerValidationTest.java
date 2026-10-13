package com.nextgen.gameaggregator.vendor.jdb.api.debit;

import com.nextgen.gameaggregator.core.engine.wallet.debit.WalletDebitService;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.vendor.jdb.api.action.ActionDto;
import com.nextgen.gameaggregator.vendor.jdb.vo.CommonVo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Proves the wiring, not just the helper: a malformed Action 9 must be rejected by the controller
 * before {@code processRequest} reaches the wallet. Previously these payloads were accepted with
 * {@code 0000} and moved money (OVI-3178 / OVI-3179 / OVI-2162).
 */
@ExtendWith(MockitoExtension.class)
class JdbDebitControllerValidationTest {

    @Mock private JdbDebitContextMapper requestMapper;
    @Mock private JdbDebitResponseMapper responseMapper;
    @Mock private WalletDebitService walletService;

    private ActionDto wire(String json) {
        ActionDto dto = new ActionDto();
        dto.setAction(9);
        dto.setParams(json);
        dto.setHttpRequestLog(new HttpRequestLog());
        return dto;
    }

    private JdbDebitController controller() {
        return new JdbDebitController(requestMapper, responseMapper, walletService);
    }

    private static final String VALID_EXCEPT = """
            {"action":"9","ts":%s,"transferId":%s,"uid":"tm63tllpc","currency":"PP",
             "amount":%s,"gameRoundSeqNo":8009320445,"gType":18,"mType":18026}""";

    @Test
    void tsMissing_isRejectedWith8000_andNeverReachesTheWallet() {
        String json = "{\"action\":\"9\",\"transferId\":\"40581947\",\"uid\":\"tm63tllpc\","
                + "\"currency\":\"PP\",\"amount\":20,\"gameRoundSeqNo\":8009320445,"
                + "\"gType\":18,\"mType\":18026}";

        ResponseEntity<CommonVo> response = controller().handle(wire(json), "trace-1", new HttpRequestLog());

        assertEquals("8000", response.getBody().getStatus());
        verifyNoInteractions(walletService);
    }

    @Test
    void transferIdWithSpaces_isRejectedWith8000_andNeverReachesTheWallet() {
        String json = String.format(VALID_EXCEPT, "1789545096621", "\" 2995714945 \"", "20");

        ResponseEntity<CommonVo> response = controller().handle(wire(json), "trace-2", new HttpRequestLog());

        assertEquals("8000", response.getBody().getStatus());
        verifyNoInteractions(walletService);
    }

    @Test
    void negativeAmount_isRejectedWith8003_andNeverReachesTheWallet() {
        String json = String.format(VALID_EXCEPT, "1789545096621", "\"40581947\"", "-50");

        ResponseEntity<CommonVo> response = controller().handle(wire(json), "trace-3", new HttpRequestLog());

        assertEquals("8003", response.getBody().getStatus());
        verifyNoInteractions(walletService);
    }
}
