package com.nextgen.gameaggregator.vendor.marblex.service;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.sport.settle.SportBetResultData;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.marblex.constant.StatusCode;
import com.nextgen.gameaggregator.vendor.marblex.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.marblex.vo.CommonDataVo;
import com.nextgen.gameaggregator.vendor.marblex.vo.CommonVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class VendorService extends BaseVendorService {
    public final VendorLineService vendorLineService;
    public final AgentPlayerService agentPlayerService;
    public final VendorGameService vendorGameService;
    public final ValidationService validationService;

    @Autowired
    public VendorService(VendorLineService vendorLineService, AgentPlayerService agentPlayerService, VendorGameService vendorGameService, ValidationService validationService) {
        this.vendorLineService = vendorLineService;
        this.agentPlayerService = agentPlayerService;
        this.vendorGameService = vendorGameService;
        this.validationService = validationService;
    }

    public CommonVo mapToSuccess(String currency, BigDecimal balance) {
        return new CommonVo()
                .setStatusCode(StatusCode.SUCCESS)
                .setData(new CommonDataVo()
                        .setBalance(balance)
                        .setCurrency(currency));
    }

    public void doVerification(CommonDto dto, GameSession gameSession, boolean checkBet) throws DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException, InvalidPlayerException, InvalidCurrencyException, AuthenticationException {

        if(checkBet) {
            // validate vendor username, agent vendor line, player status, and game status
            validationService.validateEligibleBet(gameSession, gameSession.getVendorPlayerUsername());
        }else {
            // Verify vendor line is active
            vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

            // Verify agent player is active
            agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

            // Verify vendor game is active
            vendorGameService.verifyGameStatus(gameSession.getVendorGameId());
        }

        // Verify player name from dto is equal
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getPlayerId(), InvalidPlayerException::new);
    }

    public void doDataMapper(WalletRequest walletRequest, SportBetResultData sportBetResultData) {
        walletRequest.setExternalTransactionId(sportBetResultData.getExternalTransactionId());
        walletRequest.setVendorBetId(sportBetResultData.getVendorBetId());
        walletRequest.setRoundId(sportBetResultData.getRoundId());
        walletRequest.setVendorPlayerUsername(sportBetResultData.getVendorPlayerUsername());
        walletRequest.setBetAmount(sportBetResultData.getBetAmount());
        walletRequest.setNewBetAmount(sportBetResultData.getBetAmount());
        walletRequest.setWinAmount(sportBetResultData.getWinAmount());
        walletRequest.setWinLoss(sportBetResultData.getWinLoss());
        walletRequest.setEffectiveTurnover(sportBetResultData.getBetAmount());
        walletRequest.setVendorBetTime(sportBetResultData.getVendorBetTime());
        walletRequest.setVendorSettleTime(sportBetResultData.getVendorSettleTime());
        walletRequest.setBetType(sportBetResultData.getBetType());
        walletRequest.setBetStatus(sportBetResultData.getBetStatus());
    }
}
