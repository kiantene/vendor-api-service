package com.nextgen.gameaggregator.core.engine.wallet.balance;

import com.nextgen.core.api.ApiResult;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.entity.VendorCurrency;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.service.VendorCurrencyDataService;
import com.nextgen.gameaggregator.core.webclient.ClientApiResponse;
import com.nextgen.gameaggregator.core.webclient.OperatorApiAdapter;
import com.nextgen.gameaggregator.core.webclient.OperatorApiRequest;
import com.nextgen.gameaggregator.entity.couchbase.AgentMeta;
import com.nextgen.gameaggregator.entity.couchbase.GameRound;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BalanceProcessor {
    private final OperatorApiAdapter operatorApiAdapter;
    private final VendorCurrencyDataService vendorCurrencyDataService;

    public PlayerBalanceData process(String traceId, GameRound gameRound) {

        PlayerBalanceData operatorPlayerBalance = callToOperator(traceId, gameRound);

        VendorCurrency vendorCurrency = vendorCurrencyDataService.getByVendorIdAndVendorCurrencyCode(
                gameRound.getVendorId(),
                gameRound.getCurrency()
        );

        return operatorPlayerBalance.toVendorView(
                gameRound.getUsername(),
                gameRound.getCurrency(),
                vendorCurrency.getToVendorRate()
        );
    }

    private PlayerBalanceData callToOperator(String traceId, GameRound gameRound) {
        WalletBalanceDto requestDto = mapToClientRequest(traceId, gameRound.getAgentMeta());
        OperatorApiRequest apiRequest = operatorApiAdapter.toApiRequest(requestDto, gameRound.getAgentMeta().getAgentId());

        try {
            ApiResult apiResult = operatorApiAdapter.execute(apiRequest);
            apiResult.throwIfError();
            ClientApiResponse response = apiResult.parseTo(ClientApiResponse.class);

            return response.getData();
        } catch (Exception ex) {
            LogContext logContext = LogContextHolder.get();
            logContext.setException(ex);
        }
        // if operator exception then return default balance
        return PlayerBalanceData.getDefault(gameRound.getUsername(), gameRound.getCurrency());
    }

    private WalletBalanceDto mapToClientRequest(String traceId, AgentMeta agentMeta) {
        WalletBalanceDto dto = new WalletBalanceDto();

        dto.setTraceId(traceId);
        dto.setUsername(agentMeta.getUsername());
        dto.setCurrency(agentMeta.getCurrency());
        dto.setToken(agentMeta.getSession());

        return dto;
    }
}
