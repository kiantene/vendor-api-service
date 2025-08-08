package com.nextgen.gameaggregator.core.engine.game.url;

import com.nextgen.core.webclient.ApiExecutor;
import com.nextgen.core.webclient.HandlerResult;
import com.nextgen.core.webclient.WebClientRequest;
import com.nextgen.gameaggregator.core.logging.LogContext;
import com.nextgen.gameaggregator.core.logging.LogContextHolder;
import com.nextgen.gameaggregator.core.signature.SignatureStrategy;
import com.nextgen.gameaggregator.core.signature.SigningStrategyType;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.core.util.VendorCredentialUtils;
import com.nextgen.gameaggregator.entity.ga.VendorLineCredential;
import org.springframework.http.MediaType;

import java.util.Map;

public abstract class AbstractGameLaunchHandler<Q, R> implements GameLaunchHandler<Q, R> {

    protected final VendorCredentialUtils credentialUtils;
    private final String vendorClassName;
    private final Class<R> responseType;
    private final SignatureStrategy signatureStrategy;

    protected AbstractGameLaunchHandler(
            VendorCredentialUtils credentialUtils,
            String vendorClassName,
            Class<R> responseType) {
        this(credentialUtils, vendorClassName, responseType, null);
    }

    protected AbstractGameLaunchHandler(
            VendorCredentialUtils credentialUtils,
            String vendorClassName,
            Class<R> responseType,
            SigningStrategyType strategyType) {

        this.credentialUtils = credentialUtils;
        this.vendorClassName = vendorClassName;
        this.responseType = responseType;
        this.signatureStrategy = (strategyType != null ? strategyType : SigningStrategyType.NO_OP).getStrategy();
    }

    @Override
    public String getActionName() {
        return "GameLaunch";
    }

    protected String sign(String payload, String secret) {
        return signatureStrategy.sign(payload, secret);
    }

    protected String sign(Object payload, String secret) {
        return signatureStrategy.sign(payload, secret);
    }

    protected final VendorCredentialAccessor credentials(Map<String, VendorLineCredential> raw) {
        return credentialUtils.of(raw);
    }

    protected AbstractGameLaunchHandler<Q, R> prepareLaunchRequest(GameLaunchContext context) {
        LogContext logContext = LogContextHolder.get();
        logContext.setLogGroup("Game");
        logContext.setType("Launcher");
        logContext.setAgentId(context.getAgentId());
        logContext.setVendorId(context.getVendorId());
        logContext.setUsername(context.getAgentPlayerUsername());

        return this;
    }

    public final HandlerResult<Q, R> execute(ApiExecutor executor, GameLaunchContext context) {
        return executor.execute(context, this);
    }

    @Override
    public final String getVendorClassName() {
        return this.vendorClassName;
    }

    @Override
    public final Class<R> getResponseType() {
        return responseType;
    }

    @Override
    public MediaType getContentType() {
        return MediaType.APPLICATION_FORM_URLENCODED;
    }

    @Override
    public void beforeSend(WebClientRequest<Q> request) {
        LogContext logContext = LogContextHolder.get();

        logContext.setApiBody(request.body());
        logContext.setApiUrl(request.url());
    }
}
