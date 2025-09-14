package com.nextgen.gameaggregator.core.webclient;

import io.netty.channel.unix.Errors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.util.retry.Retry;

import java.time.Duration;

@Component
@Slf4j
public class WebClientRetryPolicy {

    private int maxAttempts;
    private Duration maxBackoff;
    private Duration interval;
    private double jitterFactor;

    public WebClientRetryPolicy() {
        this.maxAttempts = 3;
        this.maxBackoff = Duration.ofSeconds(1);
        this.interval = Duration.ofMillis(300);
        this.jitterFactor = 0.5;
    }

    public static WebClientRetryPolicy getDefault() {
        return new WebClientRetryPolicy();
    }

    public Retry retryWhen(String path) {
        return Retry.fixedDelay(this.maxAttempts, this.interval)
                .maxBackoff(this.maxBackoff)
                .jitter(this.jitterFactor)
                .filter(this::shouldRetry)
                .doBeforeRetry(signal -> this.retryLog(path, signal))
                .onRetryExhaustedThrow((spec, signal) -> signal.failure());
    }

    private boolean shouldRetry(Throwable ex) {
        return ex instanceof java.io.IOException
            || ex instanceof Errors.NativeIoException
            || (ex.getCause() instanceof java.io.IOException);
    }

    private void retryLog(String path, Retry.RetrySignal signal) {
        log.warn("[{}] Retrying attempt {} due to: {}",
                path,
                signal.totalRetries() + 1,
                signal.failure());
    }
}
