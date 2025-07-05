package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.gameaggregator.core.common.ClientRequestAuth;
import com.nextgen.gameaggregator.core.common.ContextEnricher;
import com.nextgen.gameaggregator.core.common.ContextValidator;
import com.nextgen.gameaggregator.core.common.OperatorApiCaller;
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
                                  PromoPayoutMapper mapper) {

        this.validator = validator;
        this.enricher = enricher;
        this.processor = processor;
        this.mapper = mapper;
        this.operatorApiCaller = new OperatorApiCaller(EndPoints.PROMO_PAYOUT);
    }

    @Override
    public PlayerBalanceData process(PromoPayoutContext context) {
        validator.validateOrThrow(context);
        enricher.enrich(context);
        processor.process(context);

        PromoPayoutRequest clientRequest = this.buildOperatorRequest(context);
        ClientRequestAuth clientRequestAuth = new ClientRequestAuth(context.getAgentId(), clientRequest);

        try {
            ClientBalanceResponse response = operatorApiCaller.post(clientRequestAuth, clientRequest);
            processor.onSuccess(context, response);
            return response.getData();
        } catch (Exception ex) {
            processor.onError(context, ex);
            throw ex;
        }
    }

    private PromoPayoutRequest buildOperatorRequest(PromoPayoutContext context) {
        PromoPayoutRequest promoPayoutRequest = mapper.toPromoPayoutRequest(context);
        // validate the request object
        return promoPayoutRequest;
    }
}
