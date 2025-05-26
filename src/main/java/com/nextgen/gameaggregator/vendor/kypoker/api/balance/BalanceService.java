package com.nextgen.gameaggregator.vendor.kypoker.api.balance;

import com.nextgen.gameaggregator.core.WalletRequestService;
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
    private final WalletRequestService walletRequestService;
    private final HttpService httpService;

    public BalanceService(GameServiceImpl gameService,
                          WalletService walletService,
                          ValidationService validationService,
                          GameSessionService gameSessionService,
                          WalletRequestService walletRequestService,
                          HttpService httpService) {

        this.gameService = gameService;
        this.walletService = walletService;
        this.validationService = validationService;
        this.gameSessionService = gameSessionService;
        this.walletRequestService = walletRequestService;
        this.httpService = httpService;
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

            ResponseObjectDto d = new ResponseObjectDto();

            d.setCode(ResponseCodes.SUCCESS);
            d.setAccount(gameSession.getVendorPlayerUsername());
            d.setMoney(balance);

            // Construct VO
            vo.setM(EndPoints.LAUNCH_GAME);
            vo.setS(ResponseCodes.GET_BALANCE);
            vo.setD(d);

        } catch(InvalidPlayerException invalidPlayerException){
            ResponseObjectDto d = new ResponseObjectDto();
            d.setCode(ResponseCodes.CODE2);
            vo.setM(EndPoints.LAUNCH_GAME);
            vo.setS(ResponseCodes.GET_BALANCE);
            vo.setD(d);

        } catch(InvalidRequestException invalidRequestException){
            ResponseObjectDto d = new ResponseObjectDto();
            d.setCode(ResponseCodes.INVALID_REQUEST);
            vo.setM(EndPoints.LAUNCH_GAME);
            vo.setS(ResponseCodes.GET_BALANCE);
            vo.setD(d);

        } catch(AuthenticationException authenticationException){
            ResponseObjectDto d = new ResponseObjectDto();
            d.setCode(10);
            vo.setM(EndPoints.LAUNCH_GAME);
            vo.setS(ResponseCodes.GET_BALANCE);
            vo.setD(d);

        } catch (Exception e){
            ResponseObjectDto d = new ResponseObjectDto();
            d.setCode(13);
            vo.setM(EndPoints.LAUNCH_GAME);
            vo.setS(ResponseCodes.GET_BALANCE);
            vo.setD(d);

        } finally {
            httpService.end(httpRequestLog, vo);
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
        ValidationUtils.isEquals(gameSession.getCurrencyCode(), dto.getCurrency());
    }
}
