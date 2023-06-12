package com.nextgen.gameaggregator.vendor.booongo.service;

import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.BaseVendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@Slf4j
public class VendorService extends BaseVendorService {
    public static String generateGameUrl(String API_URL, String PROJECT_NAME, String token, String platform, String gameCode, String lang, String WL){
        String gameUrl = API_URL + PROJECT_NAME + "/game.html";

        //combine those string to form url
        gameUrl = gameUrl + "?token=" + token + "&platform=" + platform + "&game=" + gameCode + "&lang=" + lang + "&WL=" + WL;

//        gameUrl = MessageFormat.format(gameUrl, token, platform, gameCode, lang);

        return gameUrl;
    }

    @Override
    public ResultType calculateResultType(BigDecimal betAmount, BigDecimal winAmount, BigDecimal jackpotAmount, Integer isBet) {
        winAmount = Optional.ofNullable(winAmount).orElse(BigDecimal.ZERO);
        jackpotAmount = Optional.ofNullable(jackpotAmount).orElse(BigDecimal.ZERO);

        boolean isWinAmountMoreThanZero = winAmount.compareTo(BigDecimal.ZERO) > 0;
        boolean isJackpotAmountMoreThanZero = jackpotAmount.compareTo(BigDecimal.ZERO) > 0;

        ResultType resultType = ResultType.BET_LOSE; //default is lose

        if(isBet == 1){ //it is normal bet
            //set BET_WIN when transaction data is win
            if (isWinAmountMoreThanZero || isJackpotAmountMoreThanZero) {
                resultType = ResultType.BET_WIN;
            }
        }else{ //it is free spin or jackpot
            resultType = ResultType.WIN; // will set it as WIN no matter it is win or lose
        }

        return resultType;
    }
}
