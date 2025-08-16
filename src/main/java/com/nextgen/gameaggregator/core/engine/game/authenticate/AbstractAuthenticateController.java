package com.nextgen.gameaggregator.core.engine.game.authenticate;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.mapping.VendorRequestMapper;
import com.nextgen.gameaggregator.core.mapping.VendorResponseMapper;
import org.springframework.http.ResponseEntity;

public abstract class AbstractAuthenticateController<Q, R> {
    protected final VendorRequestMapper<AuthenticateContext, Q> requestMapper;
    protected final VendorResponseMapper<AuthenticateContext, R> responseMapper;
    protected final AuthenticateService authenticateService;

    protected AbstractAuthenticateController(VendorRequestMapper<AuthenticateContext, Q> requestMapper,
                                             VendorResponseMapper<AuthenticateContext, R> responseMapper,
                                             AuthenticateService authenticateService) {
        this.requestMapper = requestMapper;
        this.responseMapper = responseMapper;
        this.authenticateService = authenticateService;
    }

    protected ResponseEntity<R> doAuthenticate(Q request) {
        AuthenticateContext context = mapToInternal(request);
        PlayerBalanceData balanceData = authenticateService.process(context);
        return ResponseEntity.ok(mapToVendor(context, balanceData));
    }

    protected AuthenticateContext mapToInternal(Q request) {
        return requestMapper.toInternal(request);
    }

    protected R mapToVendor(AuthenticateContext context, PlayerBalanceData balanceData) {
        return responseMapper.toVendor(context, balanceData);
    }
}
