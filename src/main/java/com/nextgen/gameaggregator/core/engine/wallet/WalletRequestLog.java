package com.nextgen.gameaggregator.core.engine.wallet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextService;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;

/**
 * Request-log handling for the debit/credit engine, so a vendor on this engine gets a complete
 * api_request_log row — including {@code operatorUrl} / {@code operatorData} /
 * {@code operatorResponse} — without writing any logging code of its own (OVI-3176).
 *
 * <h2>The two arrangements this reconciles</h2>
 * <b>Framework-owned</b> is the normal one, and what the bet/result family does: the wallet
 * wrapper creates the {@link HttpRequestLog}, registers it on the {@link LogContext}, and
 * {@code LogContextService.logApiRequest} — driven by {@code RequestLoggingFilter} at request end
 * — records the operator call on it and publishes it. See {@code WalletBetServiceWrapper} and its
 * four siblings. Until this class existed the debit/credit wrappers did none of that, so a vendor
 * on this engine got <b>no api_request_log row at all</b> unless it published one itself.
 * <p>
 * <b>Vendor-owned</b> is for a vendor whose transport prevents that. JDB multiplexes every action
 * through one encrypted endpoint, so {@code GeneralAction} creates the log via
 * {@code HttpService.start} and publishes it via {@code HttpService.end}. It must keep that log
 * rather than be handed a framework one, because the framework's is built from
 * {@code logContext.getBody()} — the raw encrypted body — whereas JDB sets the decrypted payload
 * on its own. Publishing a framework copy as well would give two rows per request.
 *
 * <h2>The contract</h2>
 * An engine calls {@link #begin} at the top of its wrapper and {@link #end} in the wrapper's
 * {@code finally}. Which arrangement applies is decided by whether a vendor called
 * {@link #declareVendorOwned}, and engines do not need to care:
 * <ul>
 *   <li><b>Vendor-owned</b> — {@code end} records the operator call and leaves publishing alone.</li>
 *   <li><b>Framework-owned</b> — {@code end} registers the log, and {@code logApiRequest} records
 *       the operator call and publishes it.</li>
 * </ul>
 * Either way the request produces exactly one row.
 */
public final class WalletRequestLog {

    /**
     * Deliberately not {@code HttpRequestLog.class.getSimpleName()}: that key is what makes
     * {@code logApiRequest} publish. A vendor-owned log registered under it would be published by
     * the framework as well as by the vendor, giving two api_request_log rows per request, and
     * {@code RequestLoggingFilter} is an unrestricted {@code OncePerRequestFilter} so the second
     * publish would always fire.
     */
    private static final String VENDOR_OWNED_KEY = "VendorOwnedHttpRequestLog";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private WalletRequestLog() {
    }

    /**
     * Declares that the vendor layer created this request's log and publishes it itself, so the
     * engine enriches it instead of creating and publishing its own.
     * <p>
     * Only needed by a vendor that cannot use the framework's arrangement. A plain
     * {@code @RestController} vendor calls nothing and gets the framework one.
     */
    public static void declareVendorOwned(LogContext logContext, HttpRequestLog httpRequestLog) {
        if (logContext == null || httpRequestLog == null) {
            return;
        }
        logContext.put(VENDOR_OWNED_KEY, httpRequestLog);
    }

    /**
     * The log this request should be recorded on: the vendor's if it declared one, otherwise a
     * fresh framework log stamped with {@code requestType}.
     */
    public static HttpRequestLog begin(LogContext logContext, String requestType) {
        if (logContext == null) {
            return null;
        }
        HttpRequestLog vendorOwned = vendorOwned(logContext);
        if (vendorOwned != null) {
            return vendorOwned;
        }
        HttpRequestLog created = LogContextService.toHttpRequestLog(logContext);
        created.setRequestType(requestType);
        return created;
    }

    /**
     * Finishes the request log. Call from the wrapper's {@code finally}, not its success path: the
     * operator url and request body are set by {@code onBeforeSend}, ahead of any failure, and a
     * rejected or timed-out operator call is the case most worth having in the log.
     */
    public static void end(LogContext logContext, HttpRequestLog httpRequestLog) {
        if (logContext == null || httpRequestLog == null) {
            return;
        }
        if (vendorOwned(logContext) == httpRequestLog) {
            // The vendor publishes this one, so logApiRequest will not see it and will not record
            // the operator call on it. Do that here.
            recordOperatorCall(logContext, httpRequestLog);
            return;
        }
        // Framework-owned: registering it is what makes logApiRequest record the operator call and
        // publish, exactly as it does for the bet/result family.
        LogContextService.updateLogContextFromHttpRequestLog(logContext, httpRequestLog);
    }

    /**
     * Records why the request failed. Call from the wrapper's {@code catch}, before rethrowing.
     *
     * <p>Needed for a vendor-owned log. Its vendor would record the failure in its own catch, but
     * on this engine the exception never reaches that catch: {@code VendorExceptionAspect} turns
     * it into the vendor's error response first, so the vendor sees a normal return and publishes
     * a log with an empty {@code error} (OVI-3176: every JDB 9999 had none). The aspect does put
     * the exception on the {@link LogContext}, but only after {@link #end} has already run.
     *
     * <p>Same shape as {@code HttpService.logError}'s general case, which is what the legacy JDB
     * services write. Write-once, so a vendor that did record its own error keeps it. A
     * framework-owned log is unaffected in the end: {@code logApiRequest} writes the error from
     * the log context when it publishes.
     */
    public static void fail(HttpRequestLog httpRequestLog, Throwable failure) {
        if (httpRequestLog == null || failure == null || httpRequestLog.getErrorMessage() != null) {
            return;
        }
        httpRequestLog.setErrorMessage(failure.toString());
        httpRequestLog.setExceptionMessage(failure.getMessage());
    }

    /** Stamps identifiers a reader needs to correlate the row. Write-once. */
    public static void enrich(HttpRequestLog httpRequestLog, String vendorTxnId, String vendorRoundId) {
        if (httpRequestLog == null) {
            return;
        }
        if (httpRequestLog.getVendorBetId() == null) {
            httpRequestLog.setVendorBetId(vendorTxnId);
        }
        if (httpRequestLog.getRoundId() == null) {
            httpRequestLog.setRoundId(vendorRoundId);
        }
    }

    private static HttpRequestLog vendorOwned(LogContext logContext) {
        if (!logContext.exists(VENDOR_OWNED_KEY)) {
            return null;
        }
        return logContext.get(VENDOR_OWNED_KEY) instanceof HttpRequestLog log ? log : null;
    }

    /**
     * Mirrors the operator-field block in {@code logApiRequest}, for the vendor-owned path that
     * never reaches it. Every field is write-once, matching that block, so a log passing through
     * both paths is unaffected by the order — and so a legacy action that already set
     * {@code operatorEndPoints} and friends directly (via {@code WalletBetAction} /
     * {@code WalletBalanceAction}) keeps its values.
     */
    private static void recordOperatorCall(LogContext logContext, HttpRequestLog httpRequestLog) {
        if (httpRequestLog.getOperatorEndPoints() == null) {
            httpRequestLog.setOperatorEndPoints(logContext.getApiUrl());
        }
        if (httpRequestLog.getOperatorStart() == null && logContext.getApiStart() > 0) {
            httpRequestLog.setOperatorStart(logContext.getApiStart());
        }
        if (httpRequestLog.getOperatorEnd() == null && logContext.getApiEnd() > 0) {
            httpRequestLog.setOperatorEnd(logContext.getApiEnd());
        }
        if (httpRequestLog.getOperatorData() == null) {
            // Both fields under the operatorData guard, matching logApiRequest rather than
            // improving on it. Note what that means: HttpRequestLog.setRequestType pre-sets
            // operatorData ONLY (never operatorResponse) for the operator request types, so such a
            // log keeps its operatorData and never receives an operatorResponse. Inert on this
            // engine — REQUEST_TYPE is not one of those types, and a vendor-owned log is returned
            // by begin() before any setRequestType runs — but the coupling is logApiRequest's, and
            // splitting it here would make the two paths disagree.
            if (logContext.getApiBody() != null) {
                httpRequestLog.setOperatorData(toLogString(logContext.getApiBody()));
            }
            if (logContext.getApiResponse() != null) {
                httpRequestLog.setOperatorResponse(toLogString(logContext.getApiResponse()));
            }
        }
    }

    /**
     * {@code HttpRequestLog} declares operatorData and operatorResponse as Strings, so write
     * Strings. An object here would give the daily api-request-logs index two shapes for one field
     * and reproduce the GA-14989 mapping failure.
     */
    private static String toLogString(Object value) {
        if (value instanceof String str) {
            return str;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception ex) {
            // Never fail a wallet call for a log field.
            return String.valueOf(value);
        }
    }
}
