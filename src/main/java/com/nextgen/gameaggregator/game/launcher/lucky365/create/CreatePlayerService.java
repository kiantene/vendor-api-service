package com.nextgen.gameaggregator.game.launcher.lucky365.create;

import com.nextgen.core.webclient.VendorApiExecutor;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreatePlayerService {
    private final VendorApiExecutor apiExecutor;
    private final CreatePlayerHandler createPlayerHandler;

    public void process(GameLaunchContext context) {
        createPlayerHandler.execute(apiExecutor, context)
                .onError(result -> {
                    Throwable ex = result.getError();
                    LogContext logContext = LogContextHolder.get();
                    logContext.setApiBody(result.getRequestObject());
                    logContext.setApiResponse(result.getRawResponse());
                    logContext.setException(ex.getClass().getName());
                    logContext.setErrorMessage(ex.getMessage());

                    Throwable cause = ex.getCause();
                    while (cause.getCause() != null && cause.getCause() != cause) {
                        cause = cause.getCause();
                    }
                    logContext.setRootCause(cause.toString());
                });
    }
}
