package com.nextgen.gameaggregator.vendor.pgsoft.api.bet;

import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.mapping.VendorRequestMapper;
import com.nextgen.gameaggregator.enums.PromoType;
import org.springframework.stereotype.Component;

/**
 * Maps PGSoft {@code Cash/TransferInOut} onto the internal promo-payout context.
 *
 * <p>{@code gameId} is PGSoft's {@code vendor_game.vendor_game_code} verbatim — no prefix or
 * conversion. {@code CashTransferInOutAction} treats it the same way on the bet path, both for the
 * {@code vendor_game} lookup and when writing the code onto the session, so the promo enricher's
 * {@code getByVendorGameCodeAndVendorId} resolves against the same value.
 *
 * <p><b>Expect benign {@code [PROMO_GAME_MISMATCH]} warnings for lobby launches.</b> A session opened
 * through the PGS Game Lobby carries {@code GameCodes.LOBBY_CODE} ({@code "0"}) as its vendor game
 * code while the request carries the real game — {@code VendorService.validateVendorGameCode} skips
 * its own check for exactly that reason. {@code PromoPayoutContextEnricher} is vendor-agnostic and
 * cannot, so it logs the difference. The payout and the {@code gameCode} forwarded to the operator
 * are still correct; filter these out before alerting on the tag.
 */
@Component("pgsoftPromoPayoutRequestMapper")
public class PromoRequestMapper implements VendorRequestMapper<PromoPayoutContext, CashTransferInOutDto> {
    @Override
    public PromoPayoutContext toInternal(CashTransferInOutDto vendorRequest) {
        return PromoPayoutContext.builder()
                .idempotencyKey(vendorRequest.getTransactionId())
                .vendorTransactionId(vendorRequest.getTransactionId())
                .vendorPlayerUsername(vendorRequest.getPlayerName())
                .vendorCurrency(vendorRequest.getCurrencyCode())
                // promo payout history
                .vendorCampaignCode(vendorRequest.getFreeGameId().toString())
                .vendorPayoutAmount(vendorRequest.getTransferAmount())
                .vendorTransactionTime(vendorRequest.getUpdatedTime())
                .vendorGameCode(vendorRequest.getGameId())
                .promoType(PromoType.FREE_ROUND)
                .build();
    }
}
