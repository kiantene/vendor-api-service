package com.nextgen.gameaggregator.core.engine.promo.payout;

import com.nextgen.gameaggregator.core.common.ClientRequestAuth;
import com.nextgen.gameaggregator.core.common.ContextEnricher;
import com.nextgen.gameaggregator.core.common.ContextValidator;
import com.nextgen.gameaggregator.core.common.OperatorApiCaller;
import com.nextgen.gameaggregator.core.engine.ClientBalanceResponse;
import com.nextgen.gameaggregator.core.engine.CoreEngineProcessor;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;

@Service
public class PromoPayoutServiceImpl implements PromoPayoutService {

    private final ContextValidator<PromoPayoutContext> contextValidator;
    private final ContextEnricher<PromoPayoutContext> enricher;
    private final CoreEngineProcessor<PromoPayoutContext, ClientBalanceResponse> processor;
    private final PromoPayoutMapper mapper;
    private final Validator requestValidator;
    private final OperatorApiCaller operatorApiCaller;

    public PromoPayoutServiceImpl(PromoPayoutValidator contextValidator,
                                  PromoPayoutContextEnricher enricher,
                                  PromoPayoutProcessor processor,
                                  PromoPayoutMapper mapper,
                                  Validator requestValidator) {

        this.contextValidator = contextValidator;
        this.enricher = enricher;
        this.processor = processor;
        this.mapper = mapper;
        this.requestValidator = requestValidator;
        this.operatorApiCaller = new OperatorApiCaller(EndPoints.PROMO_PAYOUT);
    }

    @Override
    public PlayerBalanceData process(PromoPayoutContext context) {
        contextValidator.validateOrThrow(context);
        enricher.enrich(context);
        processor.process(context);

        PromoPayoutRequest clientRequest = mapper.toPromoPayoutRequest(context);
        ClientRequestAuth<PromoPayoutRequest> clientRequestAuth = new ClientRequestAuth<>(context.getAgentId(), clientRequest, requestValidator);

        try {
            ClientBalanceResponse response = operatorApiCaller.post(clientRequestAuth, clientRequest);
            processor.onSuccess(context, response);
            return response.getData();
        } catch (Exception ex) {
            processor.onError(context, ex);
            throw ex;
        }
    }
}
