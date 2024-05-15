package com.nextgen.gameaggregator.vendor.gpkasia.api.bet;

import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.VendorGameCode;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.gpkasia.constant.BetType;
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

        ResultType resultType = null;

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

                // reset value for checking credential value then assign again
                gameCode = null;

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

                if(gameCode == null){
                    throw new GameNotSupportedException();
                }
            }

            // update game code from session
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(gameCode, gameSession);

            // Verify remaining parameters (Verify against database values)
            this.doVerification(betDto, gameSession);

            //7mojo
            if(betDto.getPlatform().equals(PlatformType.SEVENMOJO) || betDto.getPlatform().equals(PlatformType.SEVENMOJOLATAM)){
                if(betDto.getIstips().equals(BetType.TIPS)){
                    // tips
                    balance = walletService.processBetResult(traceId, gameSession, betDto, ResultType.BET_LOSE, vendorService, httpRequestLog);
                }else{
                    // normal bet

                    if(betDto.getFinished().equals(BetType.UNFINISHED)){
                        // unsettled

                        BetEvent betEvent = walletService.processBet(traceId, gameSession, betDto, httpRequestLog.getRequestBody(), httpRequestLog);
                        balance = betEvent.getLastBalance();
                    }else{
                        //settled

                        resultType = getResultType(betDto);

                        balance = walletService.processBetResult(traceId, gameSession, betDto, resultType, vendorService, httpRequestLog);
                    }
                }
            }

            //turbo game
            if(betDto.getPlatform().equals(PlatformType.TURBOGAME) || betDto.getPlatform().equals(PlatformType.TURBOGAMELATAM)){
                if(betDto.getDealid().contains("place") && betDto.getFinished() == null){
                    // unsettled
                    BetEvent betEvent = walletService.processBet(traceId, gameSession, betDto, httpRequestLog.getRequestBody(), httpRequestLog);
                    balance = betEvent.getLastBalance();
                }else{
                    // settled
                    if(betDto.getCode().equals(BetType.POINTOUT) && betDto.getFinished().equals(BetType.FINISHED)){
                        resultType = getResultType(betDto);

                        balance = walletService.processBetResult(traceId, gameSession, betDto, resultType, vendorService, httpRequestLog);
                    }
                }
            }

            //bgaming
            if(betDto.getPlatform().equals(PlatformType.BGAMINGASIA) || betDto.getPlatform().equals(PlatformType.BGAMINGLATAM)){
                if(betDto.getFinished().equals(BetType.FINISHED)){
                    // if end-round

                    if(betDto.getCode().equals(BetType.POINTIN)){
                        // if place bet status mean lose
                        balance = walletService.processBetResult(traceId, gameSession, betDto, ResultType.BET_LOSE, vendorService, httpRequestLog);
                    }else{
                        // settled with win amount status(will happen zero amount when buy bonus game)
                        resultType = getResultType(betDto);

                        // settle transaction
                        balance = walletService.processBetResult(traceId, gameSession, betDto, resultType, vendorService, httpRequestLog);
                    }
                }else{
                    // not yet end(unsettled)
                    BetEvent betEvent = walletService.processBet(traceId, gameSession, betDto, httpRequestLog.getRequestBody(), httpRequestLog);
                    balance = betEvent.getLastBalance();
                }
            }

            //booming
            if(betDto.getPlatform().equals(PlatformType.BOOMING) || betDto.getPlatform().equals(PlatformType.BOOMINGLATAM)){
                if(betDto.getFinished().equals(BetType.FINISHED)){
                    // if end-round

                    if(betDto.getCode().equals(BetType.POINTIN)){
                        // if place bet status mean lose
                        balance = walletService.processBetResult(traceId, gameSession, betDto, ResultType.BET_LOSE, vendorService, httpRequestLog);
                    }else{
                        // settled with win amount status(will happen zero amount when buy bonus game)
                        resultType = getResultType(betDto);

                        // settle transaction
                        balance = walletService.processBetResult(traceId, gameSession, betDto, resultType, vendorService, httpRequestLog);
                    }
                }else{
                    // not yet end(unsettled)
                    BetEvent betEvent = walletService.processBet(traceId, gameSession, betDto, httpRequestLog.getRequestBody(), httpRequestLog);
                    balance = betEvent.getLastBalance();
                }
            }

            vo.setCodeMsg(ResponseCodes.SUCCESS);

            // check the code value to define it is deducted or gain money
            Double money = betDto.getCode().equals(BetType.POINTIN) ? (betDto.getMoney() * -1.00) : betDto.getMoney();

            betDataVo.setDealid(betDto.getDealid());
            betDataVo.setTimestamp(String.valueOf(VendorService.getCurrentTime()));
            betDataVo.setMoney(money);
            betDataVo.setCash(balance.setScale(2, RoundingMode.DOWN).toString());

            vo.setData(betDataVo);
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

            // check finished param when it is not tips
            if(dto.getIstips().equals("0")){
                Optional.ofNullable(dto.getFinished()).orElseThrow(InvalidRequestException::new);
            }

            Optional.ofNullable(dto.getDealid()).orElseThrow(InvalidRequestException::new);
        }

        // if turbo game platform will check finished param when end-round
        if(dto.getPlatform().equals(PlatformType.TURBOGAME) || dto.getPlatform().equals(PlatformType.TURBOGAMELATAM)){
            Optional.ofNullable(dto.getDealid()).orElseThrow(InvalidRequestException::new);

            if(dto.getCode().equals("1") && dto.getDealid().contains("settle")){
                Optional.ofNullable(dto.getFinished()).orElseThrow(InvalidRequestException::new);
            }
        }

        // if bgaming platform will check finished param
        if(dto.getPlatform().equals(PlatformType.BGAMINGASIA) || dto.getPlatform().equals(PlatformType.BGAMINGLATAM)){
            Optional.ofNullable(dto.getFinished()).orElseThrow(InvalidRequestException::new);

            // check the dealid if it is not lose game in buy finish game
            if(!(dto.getMoney() == 0.0 && dto.getCode().equals("1") && dto.getFinished().equals("1"))){
                Optional.ofNullable(dto.getDealid()).orElseThrow(InvalidRequestException::new);
            }
        }

        // if booming platform will check root_dealid & root_roundid
        if(dto.getPlatform().equals(PlatformType.BOOMING) || dto.getPlatform().equals(PlatformType.BOOMINGLATAM)){
            Optional.ofNullable(dto.getRoot_dealid()).orElseThrow(InvalidRequestException::new);
            Optional.ofNullable(dto.getRoot_roundid()).orElseThrow(InvalidRequestException::new);
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

    private ResultType getResultType(BetDto dto){
        ResultType resultType = ResultType.WIN; // Default value is win

        //7mojo & turbo game
        if(dto.getPlatform().equals(PlatformType.SEVENMOJO) || dto.getPlatform().equals(PlatformType.SEVENMOJOLATAM) || dto.getPlatform().equals(PlatformType.TURBOGAME) || dto.getPlatform().equals(PlatformType.TURBOGAMELATAM)){
            if(dto.getMoney() == 0.0 && dto.getCode().equals("1")){
                resultType = ResultType.END;
            }
        }

        //bgaming
        if(dto.getPlatform().equals(PlatformType.BGAMINGASIA) || dto.getPlatform().equals(PlatformType.BGAMINGLATAM)){
            // bgaming may happen lose in buy bonus game
            if(dto.getMoney() == 0.0 && dto.getDealid() == null){
                resultType = ResultType.END;
            }
        }

        //booming
        if(dto.getPlatform().equals(PlatformType.BOOMING) || dto.getPlatform().equals(PlatformType.BOOMINGLATAM)){
            // booming may happen lose in buy free spin game
            if(dto.getMoney() == 0.0){
                resultType = ResultType.END;
            }
        }

        return resultType;
    }
}
