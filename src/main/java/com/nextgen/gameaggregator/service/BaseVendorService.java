package com.nextgen.gameaggregator.service;
import com.nextgen.gameaggregator.entity.ga.BetInformation;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.entity.ga.VendorGame;
import com.nextgen.gameaggregator.entity.ga.custom.BetPreprocess;
import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.exception.GameNotSupportedException;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public abstract class BaseVendorService {
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private GameSessionService gameSessionService;

    @Getter
    private final BetPreprocess betPreprocess = new BetPreprocess();

    public BigDecimal calculateWinLoss(BetInformation betInfo) {
        BigDecimal betAmount = betInfo.getBetAmount();
        BigDecimal winAmount = Optional.ofNullable(betInfo.getWinAmount()).orElse(BigDecimal.ZERO);

        // According to Justin, we will not add jackpotAmount into winloss as game vendor does not include jackpotAmount in GGR calculations
        // BigDecimal jackpotAmount = Optional.ofNullable(betInfo.getJackpotAmount()).orElse(BigDecimal.ZERO);

        return winAmount.subtract(betAmount);
    }

    public BigDecimal calculateEffectiveTurnover(BetInformation betInfo) {

        BigDecimal effectiveTurnover = betInfo.getEffectiveTurnover();

        //if in the end betData still have null/0 effectiveTurnover, will be using betAmount as effectiveTurnover
        if (effectiveTurnover == null || effectiveTurnover.compareTo(BigDecimal.ZERO) == 0) {
            effectiveTurnover = betInfo.getBetAmount();
        }

        return effectiveTurnover;
    }

    //calculate ResultType for sending to operator
    public ResultType calculateResultType(BigDecimal betAmount, BigDecimal winAmount, BigDecimal jackpotAmount, boolean isBet) {

        winAmount = Optional.ofNullable(winAmount).orElse(BigDecimal.ZERO);
        jackpotAmount = Optional.ofNullable(jackpotAmount).orElse(BigDecimal.ZERO);

        boolean isWinAmountMoreThanZero = winAmount.compareTo(BigDecimal.ZERO) > 0;
        boolean isJackpotAmountMoreThanZero = jackpotAmount.compareTo(BigDecimal.ZERO) > 0;

        ResultType resultType = (isBet) ? ResultType.BET_LOSE : ResultType.END;

        if (isWinAmountMoreThanZero || isJackpotAmountMoreThanZero) {
            resultType = (isBet) ? ResultType.BET_WIN : ResultType.WIN;
        }

        return resultType;
    }

    public boolean shouldRejectCancelRequest() {
        //Temporary only BGAMING, SpadeGaming, EvoNetent need to accept cancel request
        return true;
    }

    public SettledBet updateSettleBetDataBeforeInsertToKafka(SettledBet settledBet, String rawData) {

        return settledBet;
    }

    public GameSession verifyAndRegenerateNewVendorGameCodeForGameSession(String vendorGameCode, GameSession gameSession) throws GameNotSupportedException {

        //if vendorGameCode is not matched with gameSession vendorGameCode, then regenerate the new vendorGameCode details
        if (vendorGameCode != gameSession.getVendorGameCode()) {
            VendorGame vendorGame = vendorGameService.getByVendorGameCodeAndVendorId(vendorGameCode, gameSession.getVendorId());
            gameSession.setGameCode(vendorGame.getCode());
            gameSession.setVendorGameId(vendorGame.getId());
            gameSession.setVendorGameCode(vendorGame.getVendorGameCode());
            gameSession.setGameCategoryId(vendorGame.getGameCategory().getId());
            gameSessionService.updateSession(gameSession);
        }

        return gameSession;
    }

    public void verifyIsPreProcessingVendorGame(Integer vendorGameId) throws GameNotSupportedException {
        VendorGame vendorGame = vendorGameService.getByGameId(vendorGameId);
        if(vendorGame.getBetDataPreprocessing()==1){
            betPreprocess.setIsPreProcessBet(true);
        }
    }

    public List<UnsettledBet> getVendorClassFileUnsettledBetList() {
        return Collections.emptyList();
    }
}
