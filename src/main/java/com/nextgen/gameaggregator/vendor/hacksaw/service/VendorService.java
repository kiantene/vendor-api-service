package com.nextgen.gameaggregator.vendor.hacksaw.service;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.VendorGameCode;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.GameNotSupportedException;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.service.VendorGameCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class VendorService extends BaseVendorService {
    @Autowired
    private VendorGameCodeService vendorGameCodeService;

    public static String generateGameUrl(String apiUrl, MultiValueMap<String, String> parameters) {
        // form query string
        String queryString = "";
        List<String> values = new ArrayList<>();
        for (String key : parameters.keySet()) {
            values.add(key + "=" + parameters.getFirst(key));
        }

        String loginUrl = apiUrl + "?" + String.join("&", values);

        return loginUrl;
    }

    public ResultType calculateResultType(BigDecimal betAmount, BigDecimal winAmount, BigDecimal jackpotAmount, boolean isBet, BetStatus betStatus) {

        winAmount = Optional.ofNullable(winAmount).orElse(BigDecimal.ZERO);
        jackpotAmount = Optional.ofNullable(jackpotAmount).orElse(BigDecimal.ZERO);

        boolean isWinAmountMoreThanZero = winAmount.compareTo(BigDecimal.ZERO) > 0;
        boolean isJackpotAmountMoreThanZero = jackpotAmount.compareTo(BigDecimal.ZERO) > 0;

        ResultType resultType = (isBet) ? ResultType.BET_LOSE :
                (betStatus == BetStatus.UNSETTLED) ? ResultType.LOSE : ResultType.END;

        if (isWinAmountMoreThanZero || isJackpotAmountMoreThanZero) {
            resultType = (isBet) ? ResultType.BET_WIN : ResultType.WIN;
        }

        return resultType;
    }

    public void verifyVendorGameCode(GameSession gameSession, String gameId) throws GameNotSupportedException {
        VendorGameCode vendorGameCode = vendorGameCodeService.getByVendorGameIdAndPlatformIdAndLanguageId(gameSession.getVendorGameId(), gameSession.getPlatformId(), gameSession.getLanguageId());
        if (!vendorGameCode.getBetGameCode().equals(gameId)) {
            throw new GameNotSupportedException();
        }
    }
}
