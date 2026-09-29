package com.nextgen.gameaggregator.vendor.pgsoft.service;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutService;
import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.core.mapping.VendorRequestMapper;
import com.nextgen.gameaggregator.core.mapping.VendorResponseMapper;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.vendor.pgsoft.api.bet.CashTransferInOutDto;
import com.nextgen.gameaggregator.vendor.pgsoft.api.bet.CashTransferInOutVo;
import com.nextgen.gameaggregator.vendor.pgsoft.api.bet.PromoRequestMapper;
import com.nextgen.gameaggregator.vendor.pgsoft.api.bet.PromoResponseMapper;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.pgsoft.vo.ResponseVo;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PGSoftPromoPayoutService {

    private final VendorRequestMapper<PromoPayoutContext, CashTransferInOutDto> promoRequestMapper;
    private final VendorResponseMapper<PromoPayoutContext, CashTransferInOutVo> promoResponseMapper;
    private final PromoPayoutService promoPayoutService;

    public PGSoftPromoPayoutService(PromoRequestMapper promoRequestMapper,
                                    PromoResponseMapper promoResponseMapper,
                                    PromoPayoutService promoPayoutService) {

        this.promoRequestMapper = promoRequestMapper;
        this.promoResponseMapper = promoResponseMapper;
        this.promoPayoutService = promoPayoutService;
    }

    public boolean isPromoPayout(CashTransferInOutDto dto) {
        // Transaction type:
        // 106: BetPayout
        // 400: BonusToCash
        // 403: FreeGameToCash
        String input = dto.getTransactionId();

        Pattern pattern = Pattern.compile("-403-");
        Matcher matcher = pattern.matcher(input);
        return matcher.find();
    }

    @VendorExceptionHandler(className = Endpoints.CLASS_NAME)
    public ResponseEntity<ResponseVo<CashTransferInOutVo>> doPromoPayout(CashTransferInOutDto dto, HttpRequestLog httpRequestLog) {
        ResponseVo<CashTransferInOutVo> parentResponseVo = new ResponseVo<>();

        PromoPayoutContext promoPayoutContext = promoRequestMapper.toInternal(dto);
        httpRequestLog.setId(promoPayoutContext.getTraceId()); // promo payout will start sending traceId without hyphens
        promoPayoutContext.setHttpRequestLog(httpRequestLog);

        PlayerBalanceData playerBalanceData;
        try {
            playerBalanceData = promoPayoutService
                    .initialise(promoPayoutContext)
                    .process(promoPayoutContext);
        } catch (DuplicateRequestException ex) {
            // Answered here rather than through PGSoftExceptionMapper because the response PG SOFT expects
            // for a duplicate is partly the request's own: real_transfer_amount is validated against what
            // they sent, and updated_time is echoed the same way a first-time payout echoes it. The mapper
            // is handed only the exception and can reach neither.
            return duplicateOf(dto, ex);
        }

        CashTransferInOutVo responseVo = promoResponseMapper.toVendor(promoPayoutContext, playerBalanceData);
        responseVo.setRealTransferAmount(dto.getRealTransferAmount());

        parentResponseVo.setData(responseVo);

        return ResponseEntity.ok(parentResponseVo);
    }

    /**
     * PG SOFT asks for the previous successful response when a transaction_id is resent, so repeat the
     * balance and currency recorded when that payout landed, alongside the request's own fields.
     */
    private ResponseEntity<ResponseVo<CashTransferInOutVo>> duplicateOf(CashTransferInOutDto dto,
                                                                       DuplicateRequestException ex) {
        ResponseVo<CashTransferInOutVo> parentResponseVo = new ResponseVo<>();

        // The currency is recorded only once a payout succeeds, so without one there is no previous
        // successful response to repeat and no balance worth quoting. Three states arrive here:
        //
        //   - the original is still running. Resolves on its own: once it lands, the next resend gets
        //     the real balance.
        //   - it failed after the operator was called, so the record was deliberately kept — releasing
        //     it could pay the player twice — but never enriched.
        //   - its record outlived the request that made it, the process having died in between.
        //
        // Only the first resolves. The other two answer this way to every resend, because nothing will
        // enrich that record now; a completion flag and a TTL on RequestIdempotentLog would tell them
        // apart and let them expire. Still the least wrong answer of the three: we do not know whether
        // the player was paid, so claiming either outcome could be a lie, and reporting success with
        // the zero balance the record is created with is what caused ONEAPI-535.
        //
        // A payout that failed *before* reaching the operator never reaches this branch at all —
        // PromoPayoutServiceImpl deletes the record, so the resend re-attempts the payout instead,
        // which is what PG SOFT asks for when a previous request has failed.
        if (ex.getCurrency() == null) {
            parentResponseVo.setError(ResponseCodes.PLAYER_OPERATION_IN_PROGRESS);
            return ResponseEntity.ok(parentResponseVo);
        }

        CashTransferInOutVo responseVo = new CashTransferInOutVo();
        responseVo.setCurrencyCode(ex.getCurrency());
        responseVo.setBalanceAmount(ex.getBalance());
        responseVo.setUpdatedTime(dto.getUpdatedTime());
        responseVo.setRealTransferAmount(dto.getRealTransferAmount());

        parentResponseVo.setData(responseVo);

        return ResponseEntity.ok(parentResponseVo);
    }
}
