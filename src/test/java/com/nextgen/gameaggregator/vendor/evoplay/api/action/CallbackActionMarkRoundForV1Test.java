package com.nextgen.gameaggregator.vendor.evoplay.api.action;

import com.nextgen.gameaggregator.service.data.MigrationRoundDataService;
import com.nextgen.gameaggregator.vendor.evoplay.config.EvoplayConfig;
import com.nextgen.gameaggregator.vendor.evoplay.constant.ActionName;
import com.nextgen.gameaggregator.vendor.evoplay.dto.CallbackDto;
import com.nextgen.gameaggregator.vendor.evoplay.dto.DataDto;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * GA-15041: only the bet may claim a round for v1. A win or refund landing on a v1 instance
 * during cutover config skew must not pin a round whose bet was handled by v2.
 */
public class CallbackActionMarkRoundForV1Test {

    private static final String ROUND_ID = "2044577169934";

    private final MigrationRoundDataService migrationRoundDataService = mock(MigrationRoundDataService.class);
    private final CallbackAction callbackAction = new CallbackAction();

    public CallbackActionMarkRoundForV1Test() {
        ReflectionTestUtils.setField(callbackAction, "migrationRoundDataService", migrationRoundDataService);
    }

    private static CallbackDto callback(String action, String roundId) {
        DataDto data = new DataDto();
        data.setRound_id(roundId);

        CallbackDto dto = new CallbackDto();
        dto.setName(action);
        dto.setData(data);
        return dto;
    }

    @Test
    void betClaimsTheRound() {
        callbackAction.markRoundForV1(callback(ActionName.bet.name(), ROUND_ID));

        verify(migrationRoundDataService).markOnRoundOpen(EvoplayConfig.CLASS_NAME, ROUND_ID);
        verify(migrationRoundDataService, never()).touchMarker(any(), any());
    }

    @Test
    void winOnlyExtendsAnExistingClaim() {
        // The GA-15041 case: the bet went to v2, this win landed on a v1 instance.
        callbackAction.markRoundForV1(callback(ActionName.win.name(), ROUND_ID));

        verify(migrationRoundDataService).touchMarker(EvoplayConfig.CLASS_NAME, ROUND_ID);
        verify(migrationRoundDataService, never()).markOnRoundOpen(any(), any());
    }

    @Test
    void refundOnlyExtendsAnExistingClaim() {
        callbackAction.markRoundForV1(callback(ActionName.refund.name(), ROUND_ID));

        verify(migrationRoundDataService).touchMarker(EvoplayConfig.CLASS_NAME, ROUND_ID);
        verify(migrationRoundDataService, never()).markOnRoundOpen(any(), any());
    }

    @Test
    void actionNameIsMatchedCaseInsensitively() {
        callbackAction.markRoundForV1(callback("Bet", ROUND_ID));

        verify(migrationRoundDataService).markOnRoundOpen(EvoplayConfig.CLASS_NAME, ROUND_ID);
    }

    @Test
    void nonRoutingActionsTouchNothing() {
        callbackAction.markRoundForV1(callback(ActionName.init.name(), ROUND_ID));
        callbackAction.markRoundForV1(callback(ActionName.balanceincrease.name(), ROUND_ID));

        verifyNoInteractions(migrationRoundDataService);
    }

    @Test
    void aCallbackWithoutARoundIdTouchesNothing() {
        callbackAction.markRoundForV1(callback(ActionName.bet.name(), null));
        callbackAction.markRoundForV1(callback(ActionName.bet.name(), "  "));

        verifyNoInteractions(migrationRoundDataService);
    }

    @Test
    void aMalformedCallbackTouchesNothing() {
        callbackAction.markRoundForV1(null);
        callbackAction.markRoundForV1(new CallbackDto());

        verifyNoInteractions(migrationRoundDataService);
    }
}
