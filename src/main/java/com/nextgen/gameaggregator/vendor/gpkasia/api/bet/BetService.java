package com.nextgen.gameaggregator.vendor.gpkasia.api.bet;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorGameCode;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.gpkasia.constant.Credentials;
import com.nextgen.gameaggregator.vendor.gpkasia.constant.PlatformType;
import com.nextgen.gameaggregator.vendor.gpkasia.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.gpkasia.service.VendorService;
import com.nextgen.gameaggregator.vendor.gpkasia.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

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
    @Autowired
    private VendorGameCodeService vendorGameCodeService;

    public CommonVo transaction(HttpRequestLog httpRequestLog, String traceId) {
        CommonVo vo = new CommonVo();

        BetDto betDto = new BetDto();

        BetDataVo betDataVo = new BetDataVo();

        BigDecimal balance = null;

        String gameCode = null;

        try{
            betDto = HttpService.convertQueryStringToDto(httpRequestLog.getRequestBody(), BetDto.class);

            // Validate request parameters from vendor (Non-database related)
            this.doValidation(betDto);

            gameCode = betDto.getGameinfo();

            // Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(betDto.getUser());

            if(betDto.getPlatform().equals(PlatformType.SEVENMOJO) || betDto.getPlatform().equals(PlatformType.SEVENMOJOLATAM)){
                List<VendorGameCode> vendorGameCodeList = vendorService.getVendorGameCode(gameSession, gameCode);

                String runEnv = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.env);

                // check is demo game or not
                for(VendorGameCode resultSet : vendorGameCodeList){

                    if(runEnv.equalsIgnoreCase("stg")){
                        // if env is stg
                        if(resultSet.getOpenGameCode().toLowerCase().contains("_stg")) gameCode = resultSet.getOpenGameCode();
                    }else{
                        // if env is prod
                        if(!resultSet.getOpenGameCode().toLowerCase().contains("_stg")) gameCode = resultSet.getOpenGameCode();
                    }
                }

                // if env is stg but the game code does not have this value
                if(runEnv.equalsIgnoreCase("stg") && !gameCode.toLowerCase().contains("_stg")){
                    throw new GameNotSupportedException();
                }
            }

            // update game code from session
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(gameCode, gameSession);

            // Verify remaining parameters (Verify against database values)
            this.doVerification(betDto, gameSession);

            //7mojo
            if(betDto.getPlatform().equals(PlatformType.SEVENMOJO) || betDto.getPlatform().equals(PlatformType.SEVENMOJOLATAM)){
                if(betDto.getIstips().equals("1")){
                    // tips
                    balance = walletService.processBetResult(traceId, gameSession, betDto, ResultType.BET_LOSE, vendorService, httpRequestLog);
                }else{
                    ResultType resultType = betDto.getCode().equals("2") ? ResultType.BET_LOSE : ResultType.BET_WIN;

                    balance = walletService.processBetResult(traceId, gameSession, betDto, resultType, vendorService, httpRequestLog);
                }
            }

            //turbo game
            if(betDto.getPlatform().equals(PlatformType.TURBOGAME) || betDto.getPlatform().equals(PlatformType.TURBOGAMELATAM)){
                ResultType resultType = betDto.getCode().equals("2") ? ResultType.BET_LOSE : ResultType.BET_WIN;

//                balance = walletService.processBetResult(traceId, gameSession, betDto, resultType, vendorService, httpRequestLog);

                if(betDto.getCode().equals("2")){
                    balance = walletService.processBetResult(traceId, gameSession, betDto, resultType, vendorService, httpRequestLog);

                    vo.setCodeMsg(ResponseCodes.SUCCESS);

                    // check the code value to define it is deducted or gain money
                    Double money = betDto.getCode().equals("2") ? (betDto.getMoney() * -1.00) : betDto.getMoney();

                    betDataVo.setDealid(betDto.getDealid());
                    betDataVo.setTimestamp(String.valueOf(VendorService.getCurrentTime()));
                    betDataVo.setMoney(money);
                    betDataVo.setCash(balance.setScale(2, RoundingMode.DOWN).toString());

                    vo.setData(betDataVo);
                }else{
                    vo.setCodeMsg(ResponseCodes.ERROR);
                    vo.setCodeMsg(ResponseCodes.ERROR);
                }
            }

//            vo.setCodeMsg(ResponseCodes.SUCCESS);
//
//            // check the code value to define it is deducted or gain money
//            Double money = betDto.getCode().equals("2") ? (betDto.getMoney() * -1.00) : betDto.getMoney();
//
//            betDataVo.setDealid(betDto.getDealid());
//            betDataVo.setTimestamp(String.valueOf(VendorService.getCurrentTime()));
//            betDataVo.setMoney(money);
//            betDataVo.setCash(balance.setScale(2, RoundingMode.DOWN).toString());
//
//            vo.setData(betDataVo);
        }catch(InsufficientBalanceException e){
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.INSUFFICIENT_BALANCE);
        }catch(InvalidPlayerException |
               AuthenticationException |
               DisabledAgentPlayerException |
               DisabledGameException |
               DisabledVendorLineException|
               CredentialNotFoundException|
               InvalidRequestException|
               GameNotSupportedException |
               TransactionStillProcessingException |
               BetResultIdempotentViolationException e){
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.ERROR);
        }catch(Exception e){
            httpService.logError(httpRequestLog, e);
            vo.setCodeMsg(ResponseCodes.ERROR);
        }

        return vo;
    }

    private void doValidation(BetDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);

        // if 7mojo platform will check istips param
        if(dto.getPlatform().equals(PlatformType.SEVENMOJO) || dto.getPlatform().equals(PlatformType.SEVENMOJOLATAM)){
            Optional.ofNullable(dto.getIstips()).orElseThrow(InvalidRequestException::new);
        }
    }

    private void doVerification(BetDto dto, GameSession gameSession) throws InvalidPlayerException, AuthenticationException, DisabledAgentPlayerException, DisabledGameException, DisabledVendorLineException, CredentialNotFoundException, InvalidRequestException, GameNotSupportedException {
        //validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, dto.getUser());

        // Verify vendor gameCode
//        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGameinfo(), GameNotSupportedException::new);
        vendorService.verifyVendorGameCode(gameSession, dto.getGameId());

        //Verify received api_token is same with credential
        String token = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.api_token);
        ValidationUtils.isEquals(token, dto.getApi_token(), InvalidRequestException::new);

        // check platform id
        if(!PlatformType.PlatformTypeList.contains(dto.getPlatform())){
            throw new InvalidRequestException();
        }
    }
}
