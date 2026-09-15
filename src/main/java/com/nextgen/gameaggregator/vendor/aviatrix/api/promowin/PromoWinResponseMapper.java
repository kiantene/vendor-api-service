package com.nextgen.gameaggregator.vendor.aviatrix.api.promowin;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutVendorResponseMapper;
import com.nextgen.gameaggregator.vendor.aviatrix.service.VendorService;
import com.nextgen.gameaggregator.vendor.aviatrix.vo.ResponseVo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * Builds the Aviatrix {@code /transactions/promoWin} success body.
 *
 * <p>{@code createdAt} and {@code balance} are both mandatory for a 200 in the Aviatrix spec — the
 * pre-migration action omitted them whenever {@code promoWinEnabled} was false and returned a bare
 * {@code {}}, which satisfied nobody.
 */
@Component
public class PromoWinResponseMapper implements PromoPayoutVendorResponseMapper<ResponseVo> {

    /** Aviatrix quotes all money in minor units. */
    private static final BigDecimal MINOR_UNITS_PER_MAJOR = BigDecimal.valueOf(100);

    @Override
    public ResponseVo toVendor(PromoPayoutContext context, PlayerBalanceData balanceData) {
        ResponseVo responseVo = new ResponseVo();
        responseVo.setBalance(toMinorUnits(balanceData.getBalance()));
        responseVo.setCreatedAt(VendorService.returnTime());
        return responseVo;
    }

    static BigInteger toMinorUnits(BigDecimal balance) {
        if (balance == null) {
            return BigInteger.ZERO;
        }
        return balance.setScale(2, RoundingMode.DOWN)
                .multiply(MINOR_UNITS_PER_MAJOR)
                .toBigInteger();
    }
}
