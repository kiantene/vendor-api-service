package com.nextgen.gameaggregator.core.engine.wallet.result;

import com.nextgen.gameaggregator.core.exception.*;
import com.nextgen.gameaggregator.core.idempotency.DuplicateRequestGuard;
import com.nextgen.gameaggregator.core.validator.BetValidator;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class WalletBetResultValidator {
    private static final Set<ResultType> NON_BET_RESULT_TYPES = Set.of(
            ResultType.WIN,
            ResultType.LOSE,
            ResultType.END
    );

    private final DuplicateRequestGuard duplicateRequestGuard;
    private final BetValidator validator;

    public void validateRequestContext(String vendorClassName, BetResultContext context) throws DuplicateRequestException {
        final String ACTION = "betresult";
        final String idempotencyKey = context.getIdempotencyKey();

        duplicateRequestGuard.ensureNotDuplicate(vendorClassName, ACTION, idempotencyKey);
    }

    /**
     * If it requires bet processing (e.g. isBet=true, resultType=BET_WIN|BET_LOSE) then validate the following:
     * Valid game session
     * Active game session
     * Active vendor line
     * Active player
     * Currency supported
     * Active game (game level)
     * Active game (house/master agent/agent level)
     * ----------------------------------------------
     * If it is a Win (e.g. no bet amount to process, resultType=WIN), then ignore the above validation
     * because we need to settle the corresponding bet related to this win.
     * ----------------------------------------------
     * If it is an End (resultType=End), then no validation required
     */
    public void validateBusinessState(GameSession session, BetResultContext context, ResultType resultType) throws
            GameSessionExpiredException, GameTerminatedException, PlayerDisabledException, BetNotAllowedException {

        if (isNonBetResultType(resultType)) return;

        validator.validateBusinessState(session, context.getVendorPlayerUsername());
    }

    private boolean isNonBetResultType(ResultType resultType) {
        return NON_BET_RESULT_TYPES.contains(resultType);
    }
}
