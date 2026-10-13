package com.nextgen.gameaggregator.vendor.jdb.api.debit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.debit.AbstractDebitController;
import com.nextgen.gameaggregator.core.engine.wallet.debit.DebitConfig;
import com.nextgen.gameaggregator.core.engine.wallet.debit.WalletDebitService;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.jdb.api.action.ActionDto;
import com.nextgen.gameaggregator.vendor.jdb.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.jdb.validator.JdbRequestValidator;
import com.nextgen.gameaggregator.vendor.jdb.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * JDB Action 9 (debit / place bet) entry point in the new debit/credit framework.
 * <p>
 * Pre-flight guards (session lookup, agent / vendor / currency enrichment, eligibility
 * validation) live in {@link com.nextgen.gameaggregator.core.engine.wallet.debit.WalletDebitServiceWrapper
 * WalletDebitServiceWrapper} — controller stays a thin wire-to-context translator and
 * delegates to {@link #processRequest}.
 * <p>
 * Routes exceptions through {@link JdbExceptionMapper} (className {@code "jdb"}) via
 * the {@code @VendorExceptionHandler} aspect. No inline try/catch for business exceptions.
 */
@Component
@Slf4j
public class JdbDebitController extends AbstractDebitController<DebitActionDto, CommonVo> {

    public JdbDebitController(JdbDebitContextMapper requestMapper,
                              JdbDebitResponseMapper responseMapper,
                              WalletDebitService walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<CommonVo> handle(ActionDto actionDto, String traceId, HttpRequestLog httpRequestLog) {
        DebitActionDto dto = parseOrThrow(actionDto);
        // The framework reaches processRequest straight from Jackson, so the DTO's own
        // constraints have to be run here or they never execute at all.
        Optional<CommonVo> rejection = JdbRequestValidator.validate(dto);
        if (rejection.isPresent()) {
            return ResponseEntity.ok(rejection.get());
        }
        return ResponseEntity.ok(processRequest(dto));
    }

    @Override
    protected void configure(DebitConfig config, DebitActionDto request) {
        // JDB Action 9 expects an idempotent replay (same transferId) to return
        // status=0000 + prior balance, NOT the DUPLICATE_TRANSACTION wire code.
        config.returnSuccessOnDuplicate(true);
    }

    private DebitActionDto parseOrThrow(ActionDto actionDto) {
        try {
            return HttpService.convertJsonToDto(actionDto.getParams(), DebitActionDto.class);
        } catch (JsonProcessingException e) {
            // Re-throw as runtime so the framework routes to mapper.onInvalidRequestError.
            throw new InvalidRequestException(e.getMessage());
        }
    }
}
