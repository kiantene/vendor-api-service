package com.nextgen.gameaggregator.vendor.gpkasia.api.bet;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.gpkasia.constant.Credentials;
import com.nextgen.gameaggregator.vendor.gpkasia.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.gpkasia.service.VendorService;
import com.nextgen.gameaggregator.vendor.gpkasia.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Slf4j
public class BetService {

    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorService vendorService;

    public CommonVo transaction(HttpRequestLog httpRequestLog, String traceId) {
        CommonVo vo = new CommonVo();

        BetDto betDto = new BetDto();

        BetDataVo betDataVo = new BetDataVo();

        BigDecimal balance = null;

        try{
            betDto = httpService.convertQueryStringToDto(httpRequestLog.getRequestBody(), BetDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(betDto);

            // Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(betDto.getUser());

            // Verify remaining parameters (Verify against database values)
            this.doVerification(betDto, gameSession);

            //bgaming
            if(betDto.getPlatform().equals("9")){
                // lose game in one round
                if(betDto.getCode().equals("2") && betDto.getFinished().equals("1")){
                    // settle in one request
                    ResultType resultType = ResultType.BET_LOSE;;

                    balance = walletService.processBetResult(traceId, gameSession, betDto, resultType, vendorService, httpRequestLog);
                }

                // first round bet with unfinish status mean place bet (it will receive win bet request)
                if(betDto.getCode().equals("2") && betDto.getFinished().equals("0")){
                    // unsettle
                    BetEvent betEvent = walletService.processBet(traceId, gameSession, betDto, httpRequestLog.getRequestBody(), httpRequestLog);
                    balance = betEvent.getLastBalance();
                }

                // last round with win status or win in the middle of round
                if(betDto.getCode().equals("1") && betDto.getFinished().equals("1") || betDto.getCode().equals("1") && betDto.getFinished().equals("0")){
                    ResultType resultType = ResultType.WIN;;
                    balance = walletService.processBetResult(traceId, gameSession, betDto, resultType, vendorService, httpRequestLog);
                }
            }

            vo.setCodeMsg(ResponseCodes.SUCCESS);

            // check the code value to define it is deduct or gain money
            Double money = betDto.getCode().equals("2") ? (betDto.getMoney() * -1.00) : betDto.getMoney();

            betDataVo.setDealid(betDto.getDealid());
            betDataVo.setTimestamp(String.valueOf(vendorService.getCurrentTime()));
            betDataVo.setMoney(money);
            betDataVo.setCash(balance.setScale(2, RoundingMode.DOWN).toString());

            vo.setData(betDataVo);
        }catch(InsufficientBalanceException e){
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.INSUFFICIENT_BALANCE);
        }catch(Exception e){
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.ERROR);
        }

        return vo;
    }

    private void doValidation(BetDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(BetDto dto, GameSession gameSession) throws InvalidPlayerException, AuthenticationException, DisabledAgentPlayerException, DisabledGameException, DisabledVendorLineException, CredentialNotFoundException, InvalidRequestException, GameNotSupportedException {
        //validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, dto.getUser());

        // Verify vendor gameCode
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGameinfo(), GameNotSupportedException::new);

        //Verify received api_token is same with credential
        String token = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.api_token);
        ValidationUtils.isEquals(token, dto.getApi_token(), InvalidRequestException::new);
    }
}
