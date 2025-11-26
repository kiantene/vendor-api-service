package com.nextgen.gameaggregator.core.engine.wallet.balance;

import com.nextgen.core.api.ApiResult;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.entity.Currency;
import com.nextgen.gameaggregator.core.entity.VendorCurrency;
import com.nextgen.gameaggregator.core.exception.OperatorApiException;
import com.nextgen.gameaggregator.core.service.CurrencyDataService;
import com.nextgen.gameaggregator.core.service.VendorCurrencyDataService;
import com.nextgen.gameaggregator.core.webclient.ClientApiResponse;
import com.nextgen.gameaggregator.core.webclient.OperatorApiAdapter;
import com.nextgen.gameaggregator.core.webclient.OperatorApiRequest;
import com.nextgen.gameaggregator.entity.couchbase.AgentMeta;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class BalanceProcessor {
    private final OperatorApiAdapter operatorApiAdapter;
    private final CurrencyDataService currencyDataService;
    private final VendorCurrencyDataService vendorCurrencyDataService;

    public PlayerBalanceData process(String traceId, GameRound gameRound) {
        return process(
                traceId,
                gameRound.getAgentMeta(),
                gameRound.getVendorId(),
                gameRound.getUsername(),
                gameRound.getCurrency()
        );
    }

    public PlayerBalanceData process(String traceId,
                                     AgentMeta agentMeta,
                                     Integer vendorId,
                                     String vendorPlayerUsername,
                                     String vendorCurrencyCode) {

        // If encounter error, don't send to Operator
        BigDecimal toVendorRate = getToVendorRate(vendorId, agentMeta.getCurrency());

        PlayerBalanceData operatorPlayerBalance = callToOperator(traceId, agentMeta);

        return operatorPlayerBalance.toVendorView(
                vendorPlayerUsername,
                vendorCurrencyCode,
                toVendorRate
        );
    }

    private PlayerBalanceData callToOperator(String traceId, AgentMeta agentMeta) {
        WalletBalanceDto requestDto = mapToClientRequest(traceId, agentMeta);
        OperatorApiRequest apiRequest = operatorApiAdapter.toApiRequest(requestDto, agentMeta.getAgentId());

        try {
            ApiResult apiResult = operatorApiAdapter.execute(apiRequest);
            apiResult.throwIfError();
            ClientApiResponse response = apiResult.parseTo(ClientApiResponse.class);

            return response.getData();
        } catch (Exception ex) {
            throw new OperatorApiException(ex.getMessage(), ex);
        }
    }

    private WalletBalanceDto mapToClientRequest(String traceId, AgentMeta agentMeta) {
        WalletBalanceDto dto = new WalletBalanceDto();

        dto.setTraceId(traceId);
        dto.setUsername(agentMeta.getUsername());
        dto.setCurrency(agentMeta.getCurrency());
        dto.setToken(agentMeta.getSession());

        return dto;
    }

    private BigDecimal getToVendorRate(Integer vendorId, String currencyCode) {
        Currency currency = currencyDataService.getByCode(currencyCode);
        VendorCurrency vendorCurrency = vendorCurrencyDataService.getByVendorIdAndCurrencyId(vendorId, currency.getId());
        return vendorCurrency.getToVendorRate();
    }
}
