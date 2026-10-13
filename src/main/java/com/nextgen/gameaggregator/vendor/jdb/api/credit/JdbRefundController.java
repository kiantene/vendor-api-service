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
 * JDB Action 11 (cancel / rollback) entry point. The mapper sets {@code context.isRefund = true},
 * routing through the combined operator credit endpoint with {@code OperatorCreditRequest.isRefund = 1}.
 * <p>
 * Named for the wallet engine it drives, not for JDB's wire word ("cancelBet"): the class
 * name says which engine, context and config apply, while the vendor action number is carried
 * by this Javadoc and by {@code Actions.CANCEL_BET}. Convention across the JDB debit/credit
 * classes: engine family for the engine itself and for shared pieces ({@code Debit},
 * {@code Credit}), engine mode for per-operation pieces ({@code Debit}, {@code Settle},
 * {@code Refund} — see {@code CreditContext.isRefund}). Do not rename these back to JDB's
 * action words: {@code Bet} in particular collides with {@code AbstractBetController}, a
 * different framework that 19 other vendors use.
 * <p>
 * Pre-flight guards (player resolution + agent / vendor / currency enrichment) live in
 * {@link com.nextgen.gameaggregator.core.engine.wallet.credit.WalletCreditServiceWrapper
 * WalletCreditServiceWrapper}. A cancel is resolved against the debit it names, not the player's
 * session ({@code CreditSessionResolver}), so it works after the session has aged out, and a uid
 * that does not own the debit is answered {@code 7501} (OVI-3180).
 * <p>
 * A cancel naming a debit already closed — settled, or cancelled under another transferId — is
 * rejected by {@code CreditPolicy} and answered {@code 6101 CANNOT_CANCEL} by
 * {@code JdbExceptionMapper.onRollbackNotAllowed}. {@code 0000} is only ever an applied cancel or
 * a replay of the same transferId.
 */
@Component
@Slf4j
public class JdbRefundController extends AbstractCreditController<RefundActionDto, CommonVo> {

    public JdbRefundController(JdbRefundContextMapper requestMapper,
                               JdbCreditResponseMapper responseMapper,
                               WalletCreditService walletService) {
        super(requestMapper, responseMapper, walletService);
    }

    @VendorExceptionHandler(className = EndPoints.CLASS_NAME)
    public ResponseEntity<CommonVo> handle(ActionDto actionDto, String traceId, HttpRequestLog httpRequestLog) {
        RefundActionDto dto = parseOrThrow(actionDto);
        // The framework reaches processRequest straight from Jackson, so the DTO's own
        // constraints have to be run here or they never execute at all.
        Optional<CommonVo> rejection = JdbRequestValidator.validate(dto);
        if (rejection.isPresent()) {
            return ResponseEntity.ok(rejection.get());
        }
        return ResponseEntity.ok(processRequest(dto));
    }

    @Override
    protected void configure(CreditConfig config, RefundActionDto request) {
        // JDB Action 11 expects an idempotent replay (same transferId) to return
        // status=0000 + prior balance, NOT the DUPLICATE_TRANSACTION wire code.
        config.returnSuccessOnDuplicate(true);
        // JDB Action 11: A cancel closes the debits named in refTransferIds. Bind to them so a debit
        // already settled or cancelled under another transferId cannot be cancelled again
        // (OVI-2167) — 6101 CANNOT_CANCEL.
        // Without this the only idempotency key is this credit's own transferId, so a repeat
        // bearing a fresh transferId reads as a brand-new request and pays out again.
        // JDB always sends refTransferIds, so a credit naming none is malformed (8000).
        config.requireDebitReference(true);
    }

    private RefundActionDto parseOrThrow(ActionDto actionDto) {
        try {
            return HttpService.convertJsonToDto(actionDto.getParams(), RefundActionDto.class);
        } catch (JsonProcessingException e) {
            throw new InvalidRequestException(e.getMessage());
        }
    }
}
