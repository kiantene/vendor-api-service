package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.gameaggregator.core.common.ClientRequestAuth;
import com.nextgen.gameaggregator.core.common.ContextValidator;
import com.nextgen.gameaggregator.core.common.OperatorApiCallerV2;
import com.nextgen.gameaggregator.core.context.BaseEnricher;
import com.nextgen.gameaggregator.core.engine.ClientBalanceResponse;
import com.nextgen.gameaggregator.core.engine.CoreEngineProcessor;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.service.RequestService;
import org.springframework.stereotype.Service;

@Service
public class PromoPayoutServiceImpl implements PromoPayoutService {

    private final ContextValidator<PromoPayoutContext> validator;
    private final BaseEnricher<PromoPayoutContext> enricher;
    private final CoreEngineProcessor<PromoPayoutContext, ClientBalanceResponse> processor;
    private final PromoPayoutMapper mapper;
    private final ClientRequestAuth<PromoPayoutRequest> clientRequestAuth;
    private final RequestService requestService;

    public PromoPayoutServiceImpl(PromoPayoutValidator validator,
                                  PromoPayoutContextEnricher enricher,
                                  PromoPayoutProcessor processor,
                                  PromoPayoutMapper mapper,
                                  ClientRequestAuth<PromoPayoutRequest> clientRequestAuth,
                                  RequestService requestService) {

        this.validator = validator;
        this.enricher = enricher;
        this.processor = processor;
        this.mapper = mapper;
        this.clientRequestAuth = clientRequestAuth;
        this.requestService = requestService;
    }

    @Override
    public PlayerBalanceData process(PromoPayoutContext context) {
        validator.validateOrThrow(context);
        enricher.enrich(context);
        processor.process(context);
        clientRequestAuth.initialise(context.getAgentId(), EndPoints.PROMO_PAYOUT, mapper.toPromoPayoutRequest(context));

        try {
            ClientBalanceResponse response;
            if (Boolean.TRUE.equals(requestService.shouldSkipStubCall(context.getAgentPlayerUsername()))) {
                response = requestService.getClientBalanceResponse(context.getTraceId(), context.getCurrency(), context.getAgentPlayerUsername());
            } else {
                OperatorApiCallerV2 operatorApiCaller = new OperatorApiCallerV2(EndPoints.PROMO_PAYOUT);
                response = operatorApiCaller.post(clientRequestAuth);
            }
            processor.onSuccess(context, response);

            return response.getData();
        } catch (Exception ex) {
            processor.onError(context, clientRequestAuth, ex);
            throw ex;
        }
    }
}
