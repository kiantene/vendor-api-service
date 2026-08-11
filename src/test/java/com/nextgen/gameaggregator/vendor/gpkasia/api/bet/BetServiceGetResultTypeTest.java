package com.nextgen.gameaggregator.vendor.gpkasia.api.bet;

import com.nextgen.gameaggregator.core.RequestIdempotentLogService;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.RedissonService;
import com.nextgen.gameaggregator.service.SettledBetService;
import com.nextgen.gameaggregator.service.ValidationService;
import com.nextgen.gameaggregator.service.VendorGameCodeService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.vendor.gpkasia.constant.BetType;
import com.nextgen.gameaggregator.vendor.gpkasia.constant.PlatformType;
import com.nextgen.gameaggregator.vendor.gpkasia.service.VendorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * GPK API upgrade V1 hotfix (1.1.184.1): dealid is no longer sent as null for a zero-win bgaming
 * round, so getResultType() must classify a finished, zero-money round as END based on money
 * alone - not gated by dealid==null, which would otherwise never fire again and misclassify
 * every future zero-win payout as WIN.
 */
class BetServiceGetResultTypeTest {

    private BetService betService;

    @BeforeEach
    void setUp() {
        betService = new BetService(
                mock(GameSessionService.class),
                mock(VendorLineService.class),
                mock(WalletService.class),
                mock(ValidationService.class),
                mock(HttpService.class),
                mock(RedissonService.class),
                mock(SettledBetService.class),
                mock(RequestIdempotentLogService.class),
                mock(AutowireCapableBeanFactory.class),
                mock(VendorGameCodeService.class),
                mock(VendorService.class));
    }

    private BetDto bgamingDto(String platform, String finished, BigDecimal money, String dealid) {
        BetDto dto = new BetDto();
        dto.setPlatform(platform);
        dto.setFinished(finished);
        dto.setMoney(money);
        dto.setDealid(dealid);
        dto.setCode(BetType.POINTOUT);
        dto.setBRoundid("round-1");
        return dto;
    }

    private ResultType getResultType(BetDto dto) {
        return (ResultType) ReflectionTestUtils.invokeMethod(betService, "getResultType", dto);
    }

    @Test
    void finishedZeroMoney_withNonNullDealid_isEnd() {
        // The exact scenario GPK's API upgrade introduces: a payout request is now sent on every
        // loss with a real (non-null) dealid and money=0. Before the fix this stayed ResultType.WIN
        // because of the dealid==null gate.
        BetDto dto = bgamingDto(PlatformType.BGAMINGASIA, BetType.FINISHED, BigDecimal.ZERO, "019fd011bfd37fbfb76cf56bf78cac0f");

        assertThat(getResultType(dto)).isEqualTo(ResultType.END);
    }

    @Test
    void finishedZeroMoney_withNullDealid_isStillEnd() {
        // Legacy/pre-upgrade shape must keep working identically (backward compatible).
        BetDto dto = bgamingDto(PlatformType.BGAMINGASIA, BetType.FINISHED, BigDecimal.ZERO, null);

        assertThat(getResultType(dto)).isEqualTo(ResultType.END);
    }

    @Test
    void finishedNonZeroMoney_isWin() {
        BetDto dto = bgamingDto(PlatformType.BGAMINGASIA, BetType.FINISHED, new BigDecimal("5.00"), "019fd011bfd37fbfb76cf56bf78cac0f");

        assertThat(getResultType(dto)).isEqualTo(ResultType.WIN);
    }

    @Test
    void unfinished_zeroMoney_staysWin() {
        // Mini-game unfinished settled request is intentionally always treated as WIN
        // regardless of money - unaffected by this fix (finished check gates the whole branch).
        BetDto dto = bgamingDto(PlatformType.BGAMINGASIA, BetType.UNFINISHED, BigDecimal.ZERO, "019fd011bfd37fbfb76cf56bf78cac0f");

        assertThat(getResultType(dto)).isEqualTo(ResultType.WIN);
    }

    @Test
    void bgamingLatam_finishedZeroMoney_withNonNullDealid_isEnd() {
        BetDto dto = bgamingDto(PlatformType.BGAMINGLATAM, BetType.FINISHED, BigDecimal.ZERO, "019fd011bfd37fbfb76cf56bf78cac0f");

        assertThat(getResultType(dto)).isEqualTo(ResultType.END);
    }
}
