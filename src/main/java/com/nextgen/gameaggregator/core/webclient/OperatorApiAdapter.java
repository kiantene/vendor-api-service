package com.nextgen.gameaggregator.core.webclient;

import com.nextgen.core.api.ApiResult;
import com.nextgen.core.api.BlockingApiAdapter;
import com.nextgen.gameaggregator.core.common.ClientRequestService;
import com.nextgen.gameaggregator.core.common.OperatorLoggingApiAdapterLifecycle;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.operator.wallet.balance.WalletBalanceDto;
import com.nextgen.gameaggregator.operator.wallet.rollback.WalletRollbackDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
public class OperatorApiAdapter extends BlockingApiAdapter<OperatorApiRequest, ApiResult> {

    private final ClientRequestService clientRequestService;

    public OperatorApiAdapter(ClientRequestService clientRequestService) {
        super("operator-api-pool");
        this.clientRequestService = clientRequestService;
    }

    @Override
    protected ApiResult onApiSuccess(ApiResult apiResult) {
        return apiResult;
    }

    @Override
    protected ApiResult onApiError(ApiResult apiResult) {
        return apiResult;
    }

    @Override
    protected ApiResult onApiComplete(ApiResult apiResult) {
        return apiResult;
    }

    @Override
    public ApiResult execute(OperatorApiRequest apiRequest) {
        return execute(apiRequest, OperatorLoggingApiAdapterLifecycle.get());
    }

    public OperatorApiRequest toApiRequest(WalletBalanceDto requestDto, Integer agentId) {
        return clientRequestService.createOperatorApiRequest(
                requestDto.getTraceId(),
                agentId,
                requestDto.getUsername(),
                EndPoints.WALLET_BALANCE,
                requestDto,
                System.currentTimeMillis()
        );
    }

    public OperatorApiRequest toApiRequest(WalletRollbackDto requestDto, Integer agentId) {
        return clientRequestService.createOperatorApiRequest(
                requestDto.getTraceId(),
                agentId,
                requestDto.getUsername(),
                EndPoints.WALLET_ROLLBACK,
                requestDto,
                requestDto.getTimestamp()
        );
    }
}
