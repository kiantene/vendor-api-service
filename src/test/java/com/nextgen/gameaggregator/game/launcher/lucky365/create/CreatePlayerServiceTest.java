package com.nextgen.gameaggregator.game.launcher.lucky365.create;

import com.nextgen.core.webclient.HandlerResult;
import com.nextgen.core.webclient.VendorApiExecutor;
import com.nextgen.gameaggregator.core.engine.game.url.GameLaunchContext;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * ONEAPI-245: onError previously NPE'd whenever the thrown exception had no cause
 * (ex.getCause() == null), which surfaced the whole game launch as SC_UNKNOWN_ERROR
 * instead of logging the actual CreatePlayer failure.
 */
@ExtendWith(MockitoExtension.class)
class CreatePlayerServiceTest {

    @Mock
    private VendorApiExecutor apiExecutor;
    @Mock
    private VendorCredentialUtils credentialUtils;

    private CreatePlayerService createPlayerService;

    @BeforeEach
    void setUp() {
        CreatePlayerHandler createPlayerHandler = new CreatePlayerHandler(credentialUtils);
        createPlayerService = new CreatePlayerService(apiExecutor, createPlayerHandler);
        LogContextHolder.set(new LogContext());
    }

    @AfterEach
    void tearDown() {
        LogContextHolder.clear();
    }

    @SuppressWarnings("unchecked")
    private void stubExecuteToReturn(HandlerResult<CreatePlayerRequest, CreatePlayerResponse> result) {
        when(apiExecutor.execute(any(), any())).thenReturn((HandlerResult) result);
    }

    private GameLaunchContext context() {
        return GameLaunchContext.builder().build();
    }

    @Test
    void onError_exceptionHasNoCause_doesNotThrowAndLogsException() {
        RuntimeException noCauseException = new RuntimeException("vendor rejected the request");
        stubExecuteToReturn(new HandlerResult<>(noCauseException));

        assertThatCode(() -> createPlayerService.process(context())).doesNotThrowAnyException();

        LogContext logContext = LogContextHolder.get();
        assertThat(logContext.getRootCause()).isEqualTo(noCauseException.toString());
    }

    @Test
    void onError_exceptionHasCauseChain_rootCauseIsDeepestCause() {
        Throwable deepest = new IllegalStateException("deepest cause");
        Throwable middle = new RuntimeException("middle", deepest);
        RuntimeException top = new RuntimeException("top", middle);
        stubExecuteToReturn(new HandlerResult<>(top));

        assertThatCode(() -> createPlayerService.process(context())).doesNotThrowAnyException();

        LogContext logContext = LogContextHolder.get();
        assertThat(logContext.getRootCause()).isEqualTo(deepest.toString());
    }

    @Test
    void onSuccess_doesNotTouchLogContextException() {
        CreatePlayerResponse response = CreatePlayerResponse.builder().code("S100").build();
        stubExecuteToReturn(new HandlerResult<>(null, "{}", response));

        assertThatCode(() -> createPlayerService.process(context())).doesNotThrowAnyException();

        assertThat(LogContextHolder.get().getRootCause()).isNull();
    }
}
