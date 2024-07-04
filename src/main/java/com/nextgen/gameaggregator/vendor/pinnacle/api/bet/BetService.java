package com.nextgen.gameaggregator.vendor.pinnacle.api.bet;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.BetType;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.dto.MultipleBetDto;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.sport.service.SportWalletService;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.Formats;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.Action;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.ActionsTransactionDto;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.ActionsWagerInfoDto;
import com.nextgen.gameaggregator.vendor.pinnacle.service.VendorService;
import com.nextgen.gameaggregator.vendor.pinnacle.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class BetService {
    private final SportWalletService sportWalletService;
    private final GameSessionService gameSessionService;
    private final WalletRequestService walletRequestService;

    @Autowired
    public BetService(SportWalletService sportWalletService,
                      GameSessionService gameSessionService,
                      WalletRequestService walletRequestService) {

        this.sportWalletService = sportWalletService;
        this.gameSessionService = gameSessionService;
        this.walletRequestService = walletRequestService;
    }

    @NotNull
    private static List<MultipleBetDto> getMultipleBetDtos(ActionsWagerInfoDto wagerInfoDto) throws InvalidRequestException {
        int wagerNum = wagerInfoDto.getWagerNum();
        BigDecimal stake = wagerInfoDto.getStake();
        long wagerId = wagerInfoDto.getWagerId();

        List<MultipleBetDto> multipleBetList = new ArrayList<>(wagerNum);
        BigDecimal betAmount;
        try {
            betAmount = stake.divide(BigDecimal.valueOf(wagerNum), RoundingMode.UNNECESSARY);
        } catch (ArithmeticException arithmeticException) {
            throw new InvalidRequestException(arithmeticException.getMessage());
        }

        for (int i = 1; i <= wagerNum; i++) {
            MultipleBetDto multipleBetDto = new MultipleBetDto();
            multipleBetDto.setVendorBetId(wagerId + "_" + i);
            multipleBetDto.setBetAmount(betAmount);
            multipleBetList.add(multipleBetDto);
        }
        return multipleBetList;
    }

    private static String getPinnacleGameCode(ActionsWagerInfoDto wagerInfoDto) {
        return Optional.ofNullable(wagerInfoDto.getSportId())
                .map(String::valueOf)
                .orElseGet(() ->
                        Optional.ofNullable(wagerInfoDto.getLegs())
                                .filter(legs -> !legs.isEmpty())
                                .map(legs -> String.valueOf(legs.get(0).getSportId()))
                                .orElse(null)
                );
    }

    public CommonVo bet(WalletRequest walletRequest, Action action)
            throws AuthenticationException, InvalidRequestException, BetResultIdempotentViolationException,
            InsufficientBalanceException, TransactionStillProcessingException, InvalidOperatorResponseException {

        ActionsWagerInfoDto wagerInfoDto = action.getWagerInfo();
        ActionsTransactionDto transactionDto = action.getTransaction();

        String externalTransactionId = action.getId().toString();
        Long wagerId = wagerInfoDto.getWagerId();
        Long transactionId = Optional.ofNullable(transactionDto).map(ActionsTransactionDto::getTransactionId).orElse(null);
        String vendorPlayerUsername = walletRequest.getVendorPlayerUsername();

        CommonVo commonVo = new CommonVo(action.getId(), transactionId, wagerId);
        GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(vendorPlayerUsername);
        walletRequest = walletRequestService.updateByGameSession(walletRequest, gameSession);
        this.dataMapper(walletRequest, externalTransactionId, wagerInfoDto);

        if (this.isMultipleBet(wagerInfoDto)) {
            walletRequest = sportWalletService.placeMultipleBets(walletRequest);
        } else {
            walletRequest = sportWalletService.placeBet(walletRequest);
        }
        commonVo.setBalance(walletRequest.getBalanceAfter());

        return commonVo;
    }

    private void dataMapper(WalletRequest walletRequest, String externalTransactionId, ActionsWagerInfoDto wagerInfoDto) throws InvalidRequestException {
        walletRequest.setExternalTransactionId(externalTransactionId);
        walletRequest.setVendorBetId(wagerInfoDto.getWagerId().toString());
        walletRequest.setRoundId(wagerInfoDto.getWagerId().toString());
        walletRequest.setVendorGameCode(getPinnacleGameCode(wagerInfoDto));
        walletRequest.setBetAmount(wagerInfoDto.getStake());
        walletRequest.setNewBetAmount(wagerInfoDto.getStake());
        walletRequest.setEffectiveTurnover(wagerInfoDto.getStake());

        Long vendorBetTime = VendorService.convertDateTimeStringToTimestamp(wagerInfoDto.getTransactionDate(), Formats.DATE_TIME_FORMAT_T_SEPARATOR, Formats.GMT_MINUS_FOUR);
        walletRequest.setVendorBetTime(vendorBetTime);

        Integer betType = wagerInfoDto.getType().equalsIgnoreCase("PARLAY") ? BetType.PARLAY_BET.code : BetType.NORMAL_BET.code;
        walletRequest.setBetType(betType);
        walletRequest.setBetStatus(BetStatus.UNSETTLED);
        if (this.isMultipleBet(wagerInfoDto)) {
            walletRequest.setBetIds(getMultipleBetDtos(wagerInfoDto));
        }
    }

    private boolean isMultipleBet(ActionsWagerInfoDto wagerInfoDto) {
        return Objects.nonNull(wagerInfoDto.getWagerNum()) && wagerInfoDto.getWagerNum() > 1;
    }
}
