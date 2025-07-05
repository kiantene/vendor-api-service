package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.gameaggregator.core.common.*;
import com.nextgen.gameaggregator.core.engine.ClientBalanceResponse;
import com.nextgen.gameaggregator.core.engine.CoreEngineProcessor;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import org.springframework.stereotype.Service;

@Service
public class PromoPayoutServiceImpl implements PromoPayoutService {

    private final ContextValidator<PromoPayoutContext> validator;
    private final ContextEnricher<PromoPayoutContext> enricher;
    private final CoreEngineProcessor<PromoPayoutContext, ClientBalanceResponse> processor;
    private final PromoPayoutMapper mapper;
    private final OperatorApiCaller operatorApiCaller;

    public PromoPayoutServiceImpl(PromoPayoutValidator validator,
                                  PromoPayoutContextEnricher enricher,
                                  PromoPayoutProcessor processor,
                                  PromoPayoutMapper mapper,
                                  OperatorApiCaller operatorApiCaller) {

        this.validator = validator;
        this.enricher = enricher;
        this.processor = processor;
        this.mapper = mapper;
        this.operatorApiCaller = operatorApiCaller;
    }

    @Override
    public PlayerBalanceData process(PromoPayoutContext context) {
        validator.validateOrThrow(context);
        enricher.enrich(context);
        processor.process(context);

        PromoPayoutRequest clientRequest = this.buildOperatorRequest(context);
        ClientRequestAuth clientRequestAuth = new ClientRequestAuth(context.getAgentId(), clientRequest);

        try {
            ClientBalanceResponse response = operatorApiCaller.post(clientRequestAuth.getCallback(), EndPoints.PROMO_PAYOUT, clientRequestAuth.getHeaders(), clientRequest);
            processor.onSuccess(context, response);
            return response.getData();
        } catch (Exception ex) {
            processor.onError(context);
            throw ex;
        }
    }

    private PromoPayoutRequest buildOperatorRequest(PromoPayoutContext context) {
        PromoPayoutRequest promoPayoutRequest = mapper.toPromoPayoutRequest(context);
        // validate the request object
        return promoPayoutRequest;
    }
}
