package com.nextgen.gameaggregator.vendor.evoplay.api.v2.response;

import com.nextgen.gameaggregator.core.common.VendorResponsePostProcessor;
import com.nextgen.gameaggregator.core.context.VendorExceptionContext;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.wallet.balance.BalanceContext;
import com.nextgen.gameaggregator.core.engine.wallet.balance.WalletBalanceService;
import com.nextgen.gameaggregator.core.exception.mapper.VendorErrorResponse;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.dto.CallbackDto;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.vo.ResponseDataVo;
import com.nextgen.gameaggregator.vendor.evoplay.api.v2.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.evoplay.config.EvoplayConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


@Component
@Slf4j
public class EvoplayPostProcessor implements VendorResponsePostProcessor {

    private static final List<Class<?>> REQUEST_CLASSES = List.of(CallbackDto.class, Map.class);

    private final WalletBalanceService walletBalanceService;

    public EvoplayPostProcessor(WalletBalanceService walletBalanceService) {
        this.walletBalanceService = walletBalanceService;
    }

    @Override
    public VendorErrorResponse postProcessErrorResponse(VendorErrorResponse response, VendorExceptionContext context) {

        context.getAnyPresentClass(REQUEST_CLASSES).ifPresent(request -> {
            if (!(response.getBody() instanceof ResponseVo responseVo)) {
                return;
            }

            if (request instanceof Map<?, ?> formMap) {
                @SuppressWarnings("unchecked") Map<String, String> data = (Map<String, String>) formMap;
                enrichFromMap(responseVo, data);
            }
        });

        return response;
    }

    private void enrichFromMap(ResponseVo responseVo, Map<String, String> data) {
        String userName = data.get("username");
        String currency = data.get("data[currency]");
        String traceId = data.get("traceId");
        String token = data.get("token");

        if ("ok".equalsIgnoreCase(responseVo.getStatus())) {

            if (responseVo.getData() == null) {
                responseVo.setData(new ResponseDataVo());
            }
            responseVo.getData().setCurrency(currency);
            responseVo.getData().setBalance(BigDecimal.ZERO);

            if (userName != null) {
                try {
                    BalanceContext balanceContext = BalanceContext.builder()
                            .traceId(traceId)
                            .vendorPlayerUsername(userName)
                            .vendorCurrency(currency)
                            .vendorSessionToken(token)
                            .build();

                    PlayerBalanceData balanceData = walletBalanceService.process(balanceContext);

                    if (balanceData != null && balanceData.getBalance() != null) {
                        responseVo.getData().setBalance(balanceData.getBalance());
                        log.debug("Successfully enriched data for success-masked error. TraceId: {}", traceId);
                    }
                } catch (Exception e) {
                    log.error("Failed to fetch balance for status:ok response, traceId: {}", traceId, e);
                }
            }
        } else {
            responseVo.setData(null);
            log.debug("Status is error, skipping data node enrichment. TraceId: {}", traceId);
        }
    }

    @Override
    public String getVendorClassName() {
        return EvoplayConfig.CLASS_NAME;
    }
}