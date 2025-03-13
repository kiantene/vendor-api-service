package com.nextgen.gameaggregator.vendor.kypoker.api.balance;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.kypoker.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.kypoker.vo.*;
import com.nextgen.gameaggregator.vendor.kypoker.constant.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class BalanceService {

    private final GameService gameService;
    private final WalletService walletService;
    private final ValidationService validationService;
    private final GameSessionService gameSessionService;

    public BalanceService(GameServiceImpl gameService,
                          WalletService walletService,
                          ValidationService validationService,
                          GameSessionService gameSessionService) {

        this.gameService = gameService;
        this.walletService = walletService;
        this.validationService = validationService;
        this.gameSessionService = gameSessionService;
    }

    public CommonVo balance(String actionDto, String traceId, HttpRequestLog httpRequestLog, String decryptedParam) {
        // Construct VO
        CommonVo vo = new CommonVo();

        try {
            // Convert original request body into dto
            BalanceDto balanceDto = HttpService.convertQueryStringToDto(decryptedParam, BalanceDto.class);

            // 1. Validate request parameters from vendor (Non-database related)
            this.doValidation(balanceDto, traceId);

            // 2. Get vendor player details
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(balanceDto.getAccount());

            // 3. Verify remaining parameters (Verify against database values)
            this.doVerification(balanceDto, gameSession);

            // 4. Get walletBalance
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);

            dObject d = new dObject();

            d.setCode(ResponseCodes.SUCCESS);
            d.setAccount(gameSession.getVendorPlayerUsername());
            d.setMoney(balance);

            // Construct VO
            vo.setM(EndPoints.LAUNCH_GAME);
            vo.setS(ResponseCodes.GET_BET);
            vo.setD(d);

        } catch(InvalidPlayerException invalidPlayerException){
            dObject d = new dObject();
            d.setCode(10);
            vo.setM(EndPoints.LAUNCH_GAME);
            vo.setS(ResponseCodes.GET_BET);
            vo.setD(d);

        } catch (Exception e){
            dObject d = new dObject();
            d.setCode(5);
            vo.setM(EndPoints.LAUNCH_GAME);
            vo.setS(ResponseCodes.GET_BET);
            vo.setD(d);
        }
        return vo;
    }

    private void doValidation(BalanceDto dto, String traceId) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(BalanceDto dto, GameSession gameSession)
            throws InvalidPlayerException,
            DisabledAgentPlayerException,
            DisabledVendorLineException,
            DisabledGameException,
            AuthenticationException,
            InvalidRequestException
    {
        //validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, dto.getAccount());
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getAccount());
    }
}
