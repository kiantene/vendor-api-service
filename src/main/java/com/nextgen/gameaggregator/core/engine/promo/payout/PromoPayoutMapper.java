package com.nextgen.gameaggregator.core.engine.promo.payout;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PromoPayoutMapper {

    @Mapping(source = "traceId", target = "traceId")
    @Mapping(source = "vendorPlayerUsername", target = "username")
    @Mapping(source = "transactionId", target = "transactionId")
    @Mapping(source = "currency", target = "currency")
    @Mapping(source = "amount", target = "amount")
    @Mapping(source = "type", target = "type")
    @Mapping(source = "timestamp", target = "timestamp")
    PromoPayoutRequest toPromoPayoutRequest(PromoPayoutContext context);
}
