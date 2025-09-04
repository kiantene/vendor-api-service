package com.nextgen.gameaggregator.vendor.pgsoft.service;

import com.nextgen.gameaggregator.annotation.VendorExceptionHandler;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutService;
import com.nextgen.gameaggregator.core.mapping.VendorRequestMapper;
import com.nextgen.gameaggregator.core.mapping.VendorResponseMapper;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.vendor.pgsoft.api.bet.CashTransferInOutDto;
import com.nextgen.gameaggregator.vendor.pgsoft.api.bet.CashTransferInOutVo;
import com.nextgen.gameaggregator.vendor.pgsoft.api.bet.PromoRequestMapper;
import com.nextgen.gameaggregator.vendor.pgsoft.api.bet.PromoResponseMapper;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.Endpoints;
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
        PlayerBalanceData playerBalanceData = promoPayoutService.process(promoPayoutContext);
        CashTransferInOutVo responseVo = promoResponseMapper.toVendor(promoPayoutContext, playerBalanceData);
        parentResponseVo.setData(responseVo);

        return ResponseEntity.ok(parentResponseVo);
    }
}
