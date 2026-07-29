package com.nextgen.gameaggregator.custodianseamless.walletservice.rollback;

import com.nextgen.core.api.ApiRequest;
import com.nextgen.core.api.ApiResult;
import com.nextgen.gameaggregator.core.common.LoggingApiAdapterLifecycle;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.custodianseamless.exception.WalletServiceAccessKeyNotFoundException;
import com.nextgen.gameaggregator.custodianseamless.service.TransferService;
import com.nextgen.gameaggregator.entity.wallet.AccessKey;
import com.nextgen.gameaggregator.logging.TransferWalletRequestLog;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WalletRollbackServiceImpl implements WalletRollbackService {
    private static final String ACTION = "rollback";
    private final TransferService transferService;
    private final WalletRollbackApiAdapter apiAdapter;
    private final WalletRollbackRequestMapper requestMapper;

    @Override
    @Async("walletRollbackEventExecutor")
    @EventListener
    public void process(WalletRollbackContext context) {

        LogContext logContext = new LogContext();
        logContext.setLogGroup(TransferWalletRequestLog.LOG_GROUP).setType(ACTION);
        LogContextHolder.set(logContext);

        context.setTraceId(logContext.getTraceId());
        AccessKey accessKey;
        try {
            accessKey = transferService.getWalletServiceAccessKey();
        } catch (WalletServiceAccessKeyNotFoundException e) {
            throw new RuntimeException(e);
        }

        ApiRequest apiRequest = apiAdapter.ofWalletTransactionRollback(
                context.getTraceId(),
                requestMapper.toWalletRollbackRequest(context),
                accessKey.getApiKey(),
                accessKey.getApiSecret()
        );
        ApiResult apiResult = apiAdapter.execute(apiRequest, new LoggingApiAdapterLifecycle(logContext));
        apiResult.throwIfError();
    }

    @Override
    public WalletRollbackService initialise(WalletRollbackContext context) {
        return this;
    }

}
