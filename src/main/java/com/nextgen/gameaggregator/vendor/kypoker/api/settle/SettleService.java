package com.nextgen.gameaggregator.vendor.kypoker.api.settle;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.operator.wallet.service.OperatorWalletService;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.kypoker.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.kypoker.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.kypoker.constant.RoomCode;
import com.nextgen.gameaggregator.vendor.kypoker.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.kypoker.vo.ResponseObjectDto;
import com.nextgen.gameaggregator.vendor.kypoker.service.VendorService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class SettleService {
    private final GameService gameService;
    private final WalletService walletService;
    private final ValidationService validationService;
    private final GameSessionService gameSessionService;
    private final VendorService vendorService;
    private final OperatorWalletService operatorWalletService;
    private final WalletRequestService walletRequestService;

    public SettleService(GameService gameService,
                         WalletService walletService,
                         ValidationService validationService,
                         GameSessionService gameSessionService,
                         VendorService vendorService,
                         OperatorWalletService operatorWalletService,
                         WalletRequestService walletRequestService) {

        this.gameService = gameService;
        this.walletService = walletService;
        this.validationService = validationService;
        this.gameSessionService = gameSessionService;
        this.vendorService = vendorService;
        this.operatorWalletService = operatorWalletService;
        this.walletRequestService = walletRequestService;
    }
    public CommonVo settle(String actionDto, String traceId, HttpRequestLog httpRequestLog, String decryptedParam, Long timeStamp) {

        // Construct VO
        CommonVo vo = new CommonVo();
        WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);

        try {
            // Convert original request body into dto
            SettleDto settleDto = HttpService.convertQueryStringToDto(decryptedParam, SettleDto.class);

            // 1. Validate request parameters from vendor (Non-database related)
            this.doValidation(settleDto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.getLastGameSessionByVendorPlayerUsername(settleDto.getAccount());

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(settleDto, gameSession);

            settleDto.setTimeStamp(timeStamp);
            ResponseObjectDto d = new ResponseObjectDto();

            // Check game code to use normal flow or credit debit

            //Normal Flow
            if(settleDto.getRoomMode() == RoomCode.CODE2 || settleDto.getRoomMode() == RoomCode.CODE3) {
                ResultType resultType = (settleDto.getWinAmount().compareTo(BigDecimal.ZERO) > 0) ? ResultType.BET_WIN : ResultType.BET_LOSE;
                BigDecimal balance = walletService.processBetResult(traceId, gameSession, settleDto, resultType, vendorService, httpRequestLog);
                d.setMoney(balance);
            }
            //Credit Debit flow
            else if(settleDto.getRoomMode() == RoomCode.CODE1 || settleDto.getRoomMode() == RoomCode.CODE4){
                WalletRequest currentWalletRequest = new WalletRequest(walletRequest);
                vendorService.dataCreditMapper(currentWalletRequest, settleDto, gameSession);
                walletRequest = operatorWalletService.betCredit(currentWalletRequest);
                d.setMoney(walletRequest.getBalanceAfter());
            }

            d.setCode(ResponseCodes.SUCCESS);
            d.setAccount(gameSession.getVendorPlayerUsername());
            d.setRoomMode(settleDto.getRoomMode());
            d.setBetCount(settleDto.getBetCount());
            d.setTotalBet(settleDto.getTotalBet());
            d.setValidBet(settleDto.getValidBet());
            d.setTotalWithdraw(settleDto.getTotalWithdraw());
            d.setRevenue(settleDto.getRevenue());

            // Construct VO
            vo.setM(EndPoints.LAUNCH_GAME);
            vo.setS(ResponseCodes.RETURN_BALANCE);
            vo.setD(d);

        } catch (Exception e){
            ResponseObjectDto d = new ResponseObjectDto();
            vo.setM(EndPoints.LAUNCH_GAME);
            vo.setS(ResponseCodes.RETURN_BALANCE);
            d.setCode(ResponseCodes.INTERNAL_ERROR);
            vo.setD(d);
        } finally {
            vo.setHttpRequestLog(httpRequestLog);
        }
        return vo;
    }

    private void doValidation(SettleDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(SettleDto dto, GameSession gameSession) throws DisabledVendorLineException,
            DisabledAgentPlayerException, DisabledGameException, GameNotSupportedException, CurrencyNotSupportedException,
            InvalidPlayerException, AuthenticationException {

        //validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, dto.getAccount());

        // Verify vendor gameCode, currency and platform
        String[] parts = gameSession.getVendorGameCode().split("_");
        int mType = Integer.parseInt(parts[0]);
        ValidationUtils.isEquals(String.valueOf(mType), String.valueOf(dto.getGameId()), GameNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);

    }

}