package com.nextgen.gameaggregator.vendor.aviatrix.api.promowin;

import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import com.nextgen.gameaggregator.vendor.aviatrix.vo.ResponseVo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

class PromoWinResponseMapperTest {

    private final PromoWinResponseMapper mapper = new PromoWinResponseMapper();

    /**
     * Both fields are mandatory for a 200 in the Aviatrix spec. The pre-migration action returned a bare
     * {@code {}} whenever the payout toggle was off, omitting both.
     */
    @Test
    void returnsBalanceInMinorUnitsAndACreatedAt() {
        ResponseVo response = mapper.toVendor(null, balanceOf("12.34"));

        assertThat(response.getBalance()).isEqualTo(BigInteger.valueOf(1234));
        assertThat(response.getCreatedAt()).isNotBlank();
        assertThat(response.getMessage()).isNull();
        assertThat(response.hasError()).isFalse();
    }

    @Test
    void truncatesRatherThanRoundsUp() {
        assertThat(PromoWinResponseMapper.toMinorUnits(new BigDecimal("12.349")))
                .isEqualTo(BigInteger.valueOf(1234));
    }

    @Test
    void treatsAnAbsentBalanceAsZero() {
        assertThat(PromoWinResponseMapper.toMinorUnits(null)).isEqualTo(BigInteger.ZERO);
    }

    private static PlayerBalanceData balanceOf(String balance) {
        return PlayerBalanceData.getDefaultWithBalance("player-1", "USD", new BigDecimal(balance));
    }
}
