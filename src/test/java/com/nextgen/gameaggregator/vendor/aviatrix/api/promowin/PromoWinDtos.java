package com.nextgen.gameaggregator.vendor.aviatrix.api.promowin;

import java.math.BigDecimal;

/** Valid promoWin requests, varying only the promo object under test. */
final class PromoWinDtos {

    private PromoWinDtos() {
    }

    static PromoWinDto of(String promoType, String bonusId) {
        PromoDto promo = new PromoDto();
        promo.setType(promoType);
        promo.setBonusId(bonusId);

        PromoWinDto dto = new PromoWinDto();
        dto.setCid("brand-1");
        dto.setSessionToken("token-1");
        dto.setPlayerId("player-1");
        dto.setProductId("nft-aviatrix");
        dto.setTxId("tx-1");
        dto.setAmount(new BigDecimal("333"));
        dto.setCurrency("USD");
        dto.setPromo(promo);
        return dto;
    }
}
