package com.nextgen.gameaggregator.vendor.digitain.api.promowin;

import java.math.BigDecimal;

/** Valid promowin requests, varying only the operation type under test. */
final class PromoWinRequests {

    private PromoWinRequests() {
    }

    static PromoWinRequest of(String operationType) {
        PromoWinRequest request = new PromoWinRequest();
        request.setPrid(123);          // game provider id
        request.setPid("65441");       // player id
        request.setGid("736");         // game id — optional per the spec
        request.setOpt(operationType);
        request.setPwa(new BigDecimal("75.3"));
        request.setCid("EUR");         // player currency
        request.setTxid("40439c1a12cbe8c68df1");
        return request;
    }
}
