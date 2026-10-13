package com.nextgen.gameaggregator.vendor.jdb.api.credit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.core.exception.InvalidRequestException;
import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.wallet.credit.AbstractCreditController;
import com.nextgen.gameaggregator.core.engine.wallet.credit.CreditConfig;
import com.nextgen.gameaggregator.core.engine.wallet.credit.WalletCreditService;
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
 * JDB Action 10 (settle) entry point. The mapper sets {@code context.isRefund = false}, so the
 * {@code CreditProcessor} routes through the combined operator credit endpoint with
 * {@code OperatorCreditRequest.isRefund = 0}.
 * <p>
 * Pre-flight guards (session lookup + agent / vendor / currency enrichment) live in
 * {@link com.nextgen.gameaggregator.core.engine.wallet.credit.WalletCreditServiceWrapper
 * WalletCreditServiceWrapper}. Settle uses strict session lookup
 * ({@code GameSessionDataService.getGameSession}); refund mode (cancel) uses
 * {@code getOrCreate} for the refund-survives-expiry semantic.
 * <p>
 * Exception handling delegated to {@link JdbExceptionMapper} via {@code @VendorExceptionHandler}.
 */
@Component
@Slf4j
public class JdbSettleController extends AbstractCreditController<SettleActionDto, CommonVo> {

    public JdbSettleController(JdbSettleContextMapper requestMapper,
                               JdbCreditResponseMapper responseMapper,
                               WalletCreditService walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<CommonVo> handle(ActionDto actionDto, String traceId, HttpRequestLog httpRequestLog) {
        SettleActionDto dto = parseOrThrow(actionDto);
        // The framework reaches processRequest straight from Jackson, so the DTO's own
        // constraints have to be run here or they never execute at all.
        Optional<CommonVo> rejection = JdbRequestValidator.validate(dto);
        if (rejection.isPresent()) {
            return ResponseEntity.ok(rejection.get());
        }
        return ResponseEntity.ok(processRequest(dto));
    }

    @Override
    protected void configure(CreditConfig config, SettleActionDto request) {
        // JDB Action 10 expects an idempotent replay (same transferId) to return
        // status=0000 + prior balance, NOT the DUPLICATE_TRANSACTION wire code.
        config.returnSuccessOnDuplicate(true);
        // JDB rejects a settle on a round whose ledger is already ended (vendor rule).
        // Cancel/refund (Action 11) is intentionally not gated — late refund corrections
        // after settlement remain allowed.
        config.allowCreditWhenRoundEnded(false);
        // JDB Action 10: A settle closes the debits named in refTransferIds. Bind to them: reject a
        // settle that names a debit from another round (OVI-3188), or one already settled or
        // cancelled under another transferId (OVI-3185 / 3187) — 9999, since JDB has no
        // "cannot settle" code.
        // Without this the only idempotency key is this credit's own transferId, so a repeat
        // bearing a fresh transferId reads as a brand-new request and pays out again.
        // JDB always sends refTransferIds, so a credit naming none is malformed (8000).
        config.requireDebitReference(true);
    }

    private SettleActionDto parseOrThrow(ActionDto actionDto) {
        try {
            return HttpService.convertJsonToDto(actionDto.getParams(), SettleActionDto.class);
        } catch (JsonProcessingException e) {
            throw new InvalidRequestException(e.getMessage());
        }
    }
}
