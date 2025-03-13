package com.nextgen.gameaggregator.vendor.kypoker.api.settle;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.kypoker.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.kypoker.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.kypoker.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.kypoker.vo.dObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class SettleService {
    private final GameService gameService;
    private final WalletService walletService;
    private final ValidationService validationService;
    private final GameSessionService gameSessionService;
    private final VendorService vendorService;

    public SettleService(GameService gameService,
                         WalletService walletService,
                         ValidationService validationService,
                         GameSessionService gameSessionService, VendorService vendorService) {
        this.gameService = gameService;
        this.walletService = walletService;
        this.validationService = validationService;
        this.gameSessionService = gameSessionService;
        this.vendorService = vendorService;
    }
    public CommonVo settle(String actionDto, String traceId, HttpRequestLog httpRequestLog, String decryptedParam) {
        // Construct VO
        CommonVo vo = new CommonVo();

        try {
            // Convert original request body into dto
            SettleDto settleDto = HttpService.convertJsonToDto(decryptedParam, SettleDto.class);

            // 1. Validate request parameters from vendor (Non-database related)
            this.doValidation(settleDto);

            // 2. Verify session token
            GameSession gameSession = gameSessionService.getLastGameSessionByVendorPlayerUsername(settleDto.getAccount());

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(settleDto, gameSession);

            // 4. Send bet request to Operator
            // 4.1 check if player has enough balance
            // 4.2 used database constraint to check duplicate bet request based on external_transaction_id, round_id, vendor_line_id
            // 4.3 Process Bet Request
            ResultType resultType = vendorService.calculateResultType(settleDto.getBetAmount(), settleDto.getWinAmount(), settleDto.getJackpotAmount(), false);
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, settleDto, resultType, vendorService, httpRequestLog);

            SettleVo d = new SettleVo();

            d.setCode(ResponseCodes.SUCCESS);
            d.setAccount(gameSession.getVendorPlayerUsername());
            d.setMoney(balance);
            //d.setRoomMode(gameSession.get);
            d.setBetCount(1);
            d.setTotalBet(settleDto.getBetAmount());
            d.setValidBet(settleDto.getBetAmount());
            d.setTotalWithdraw(balance);
            d.setRevenue(balance);

            // Construct VO
            vo.setM(EndPoints.LAUNCH_GAME);
            vo.setS(ResponseCodes.RETURN_BALANCE);
            vo.setD(d);

        } catch (Exception e){
            dObject d = new dObject();
            vo.setM(EndPoints.LAUNCH_GAME);
            vo.setS(ResponseCodes.RETURN_BALANCE);
            vo.setD(d);
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
        int mType = Integer.parseInt(parts[1]);
        ValidationUtils.isEquals(String.valueOf(mType), String.valueOf(dto.getGameId()), GameNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);

    }

}