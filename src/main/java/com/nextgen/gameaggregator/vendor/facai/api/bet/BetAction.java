package com.nextgen.gameaggregator.vendor.facai.api.bet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.entity.VendorPlayer;
import com.nextgen.gameaggregator.enums.WinType;
import com.nextgen.gameaggregator.eventing.core.EventDispatcherSystem;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.eventing.events.BetResultEvent;
import com.nextgen.gameaggregator.eventing.events.EndRoundEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.facai.constant.Credentials;
import com.nextgen.gameaggregator.vendor.facai.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.facai.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.facai.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.facai.service.VendorService;
import com.nextgen.gameaggregator.vendor.facai.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class BetAction {

    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private VendorPlayerService vendorPlayerService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorService vendorService;

    @PostMapping(path = EndPoints.SLOT_BET)
    public CommonVo bet(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getTraceId();

        // Construct VO
        CommonVo commonVo = new CommonVo();
        BigDecimal balance = BigDecimal.valueOf(0);
        //betVo.setResult(0);
        //betVo.setMainPoints(1000.00);

        try {
            //Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            //Convert original request body into commonDto
            CommonDto commonDto = HttpService.convertQueryStringToDtoUrlDecode(body, CommonDto.class);

            //Validate request parameters from vendor (Non-database related)
            this.doValidation(commonDto);

            //TODO pending PG update core function to get appKey
            //Decrypt raw respond
            String jsonParam = vendorService.aesDecrypt(commonDto.getParams(), "Q7RaR8CUbwZ0roD2");

            //map decrypted data(string json) into balanceDto
            VendorBetDto vendorBetDto = HttpService.convertJsonToDto(jsonParam, VendorBetDto.class);

            //Validate request parameters from vendor after decrypt (Non-database related)
            this.doDecryptValidation(vendorBetDto);

            //get gameSession by player name
            VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(vendorBetDto.memberAccount);
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(vendorPlayer.getUsername());

            //Verify remaining parameters (Verify against database values)
            this.doVerification(commonDto, vendorBetDto, gameSession, jsonParam);

            //Retrieve the latest wallet balance from Operator
            balance = walletService.getBalance(traceId, gameSession);

            //check bet type
            WinType winType = this.getWinType(vendorBetDto);

            //Construct BetDto
            BetDto betDto = this.setBetDto(vendorBetDto, winType);
            //Send bet request to Operator
            //check if player has enough balance
            //used database constraint to check duplicate bet request based on external_transaction_id, round_id, vendor_line_id
            BetEvent betEvent = walletService.processBet(traceId, gameSession, betDto, body);

            //Construct WinDataDto
            WinDataDto winDataDto = this.setWinDataDto(vendorBetDto, winType);
            //Send settle request to Operator
            BetResultEvent betResultEvent = walletService.processWin(traceId, gameSession, winDataDto, body);

            //Emit event for additional asynchronous processing
            EventDispatcherSystem.emitAsync(new EndRoundEvent(betResultEvent.getBetHistory()));

            //set VO data
            //convert bigDecimal balance into double
            commonVo.setSuccessResponseCode(ResponseCodes.SUCCESS);
            commonVo.setMainPoints(betResultEvent.getLastBalance().setScale(2,RoundingMode.DOWN).doubleValue());

        } catch (AuthenticationException authenticationException) {
            commonVo.setErrorResponseCode(ResponseCodes.PLAYER_NOT_FOUND);
        } catch (InvalidDecryptionException invalidDecryptionException) {
            commonVo.setErrorResponseCode(ResponseCodes.PARAM_CONTAIN_ERROR);
        } catch (CurrencyNotSupportedException currencyNotSupportedException) {
            commonVo.setErrorResponseCode(ResponseCodes.CURRENCY_MISSING);
        } catch (InsufficientBalanceException insufficientBalanceException) {
            commonVo.setErrorResponseCode(ResponseCodes.INSUFFICIENT_BALANCE);
        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            commonVo.setErrorResponseCode(ResponseCodes.PARAM_CONTAIN_ERROR);
        } catch (DuplicateExternalTransactionIdException duplicateExternalTransactionIdException) {
            commonVo.setErrorResponseCode(ResponseCodes.REQUIRE_CANCEL_REQUEST);
        } catch (CredentialNotFoundException credentialNotFoundException) {
            commonVo.setErrorResponseCode(ResponseCodes.PARAM_CONTAIN_ERROR);
        } catch (DisabledVendorLineException disabledVendorLineException) {
            commonVo.setErrorResponseCode(ResponseCodes.PARAM_CONTAIN_ERROR);
        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            commonVo.setErrorResponseCode(ResponseCodes.PARAM_CONTAIN_ERROR);
        } catch (BetResultNotFoundException betResultNotFoundException) {
            commonVo.setErrorResponseCode(ResponseCodes.PARAM_CONTAIN_ERROR);
        } catch (InvalidPlayerException invalidPlayerException) {
            commonVo.setErrorResponseCode(ResponseCodes.PLAYER_NOT_FOUND);
        } catch (InvalidDateException invalidDateException) {
            commonVo.setErrorResponseCode(ResponseCodes.DATE_INPUT_MISSING);
        } catch (InvalidVendorLineException invalidVendorLineException) {
            commonVo.setErrorResponseCode(ResponseCodes.PARAM_CONTAIN_ERROR);
        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            commonVo.setErrorResponseCode(ResponseCodes.PARAM_CONTAIN_ERROR);
        } catch (DisabledGameException disabledGameException) {
            commonVo.setErrorResponseCode(ResponseCodes.GAME_NOT_FOUND);
        } catch (InvalidRequestException invalidRequestException) {
            commonVo.setErrorResponseCode(ResponseCodes.PARAM_CONTAIN_ERROR);
        } catch (BetNotFoundException betNotFoundException) {
            commonVo.setErrorResponseCode(ResponseCodes.PARAM_CONTAIN_ERROR);
        } catch (JsonProcessingException jsonProcessingException) {
            commonVo.setErrorResponseCode(ResponseCodes.PARAM_CONTAIN_ERROR);
        } catch (Exception exception) {
            commonVo.setErrorResponseCode(ResponseCodes.UNEXPECTED_ERROR);
        } finally {
            httpService.end(httpRequestLog, commonVo);
        }

        return commonVo;
    }

    private void doValidation(CommonDto dto) throws InvalidRequestException, CurrencyNotSupportedException {
        // General validation
        //ValidationUtils.validateRequest(dto);
        if(!vendorService.isValidString(dto.getAgentCode())) {throw new InvalidRequestException();}
        if(!vendorService.isValidString(dto.getSign())) {throw new InvalidRequestException();}
        if(!vendorService.isValidString(dto.getCurrency())) {throw new CurrencyNotSupportedException();}
        if(!vendorService.isValidStringLength(dto.getCurrency(), 3, 3)) {throw new CurrencyNotSupportedException();}
    }

    private void doDecryptValidation(VendorBetDto dto) throws InvalidRequestException, InvalidPlayerException, InvalidDateException, CurrencyNotSupportedException {
        // General validation
        //ValidationUtils.validateRequest(dto);
        if(!vendorService.isValidString(dto.getMemberAccount())) {throw new InvalidPlayerException();}
        if(!vendorService.isValidStringLength(dto.getMemberAccount(), 2, 30)) {throw new InvalidPlayerException();}
        if(dto.getBankID() == null) {throw new InvalidRequestException();}
        if(!vendorService.isValidString(dto.getCurrency())) {throw new CurrencyNotSupportedException();}
        if(!vendorService.isValidStringLength(dto.getCurrency(), 3, 3)) {throw new CurrencyNotSupportedException();}
        if(!vendorService.isValidInteger(dto.getGameID())) {throw new InvalidRequestException();}
        if(!vendorService.isValidInteger(dto.getGameType())) {throw new InvalidRequestException();}
        if(dto.getTs() == null || !vendorService.isValidTimestamp(dto.getTs())) {throw new InvalidRequestException();}
        if(dto.getIsBuyFeature() == null) {throw new InvalidRequestException();}
        if(dto.getBet() == null  || (dto.getBet().compareTo(BigDecimal.ZERO) < 0 || dto.getBet().compareTo(new BigDecimal("999999999999")) > 0)) {throw new InvalidRequestException();}
        if(dto.getWin() == null || (dto.getWin().compareTo(BigDecimal.ZERO) < 0 || dto.getWin().compareTo(new BigDecimal("999999999999")) > 0)) {throw new InvalidRequestException();}
        if(dto.getJpBet() == null || (dto.getJpBet().compareTo(BigDecimal.ZERO) < 0 || dto.getJpBet().compareTo(new BigDecimal("999999999999")) > 0)) {throw new InvalidRequestException();}
        if(dto.getJpPrize() == null || (dto.getJpPrize().compareTo(BigDecimal.ZERO) < 0 || dto.getJpPrize().compareTo(new BigDecimal("999999999999")) > 0)) {throw new InvalidRequestException();}
        if(dto.getNetWin()== null || dto.getNetWin().compareTo(new BigDecimal("999999999999")) > 0) {throw new InvalidRequestException();}
        if(!vendorService.isValidString(dto.getRecordID())) {throw new InvalidRequestException();}
        if(!vendorService.isValidStringLength(dto.getRecordID(), 1, 24)) {throw new InvalidRequestException();}
        if(!vendorService.isValidDateString(dto.getGameDate(), "yyyy-MM-dd HH:mm:ss")) {throw new InvalidDateException();}
        if(!vendorService.isValidDateString(dto.getCreateDate(), "yyyy-MM-dd HH:mm:ss")) {throw new InvalidDateException();}

    }

    private void doVerification(CommonDto commonDto, VendorBetDto vendorBetDto, GameSession gameSession, String jsonParam) throws AuthenticationException, InvalidRequestException, CurrencyNotSupportedException, InvalidPlayerException, CredentialNotFoundException, InvalidVendorLineException, DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException {

        //Verify received username is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), vendorBetDto.getMemberAccount(), InvalidPlayerException::new);

        //Verify received game id is the same from game session
        //comparison for game session value will always be using  AuthenticationException
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), Integer.toString(vendorBetDto.getGameID()), DisabledGameException::new);

        //Verify received currency is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), commonDto.getCurrency(), CurrencyNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), vendorBetDto.getCurrency(), CurrencyNotSupportedException::new);

        //Verify received Sign is the same from param value
        //MD5 encrypt
        String md5Param = "";
        try {
            md5Param = vendorService.md5(jsonParam);
        } catch (Exception exception) { // any other exception encountered
            throw new InvalidRequestException();
        }
        ValidationUtils.isEquals(md5Param, commonDto.getSign(), InvalidRequestException::new);

        //Verify received agent code is the same from credential
        String AgentCode = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.AGENT_CODE);
        ValidationUtils.isEquals(AgentCode, commonDto.getAgentCode(), InvalidRequestException::new);

        //Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        //Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        //Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());
    }

    private WinType getWinType(VendorBetDto vendorBetDto) {
        WinType winType ;

        if(vendorBetDto.getBet().compareTo(BigDecimal.ZERO) > 0 || vendorBetDto.getWin().compareTo(BigDecimal.ZERO) > 0) {
            winType = (vendorBetDto.getWin().compareTo(BigDecimal.ZERO) > 0) ? WinType.WIN : WinType.LOSE;
        } else if (vendorBetDto.getJpPrize().compareTo(BigDecimal.ZERO) > 0) {
            winType = WinType.JACKPOT;
        } else{
            winType = (vendorBetDto.getWin().compareTo(BigDecimal.ZERO) > 0) ? WinType.WIN : WinType.LOSE;
        }

        return winType;
    }

    private BetDto setBetDto(VendorBetDto vendorBetDto, WinType winType){
        BetDto betDto = new BetDto();

        betDto.setExternalTransactionId(Long.toString(vendorBetDto.getBankID()));
        betDto.setRoundId(vendorBetDto.getRecordID());
        betDto.setGameCode(Integer.toString(vendorBetDto.getGameID()));
        betDto.setEventTime(vendorBetDto.getCreateDate());

        //set bet amount according bet type
        if(winType == WinType.JACKPOT) {
            betDto.setAmount(BigDecimal.valueOf(0));
        }else{
            betDto.setAmount(vendorBetDto.getBet());
        }

        return betDto;
    }

    private WinDataDto setWinDataDto(VendorBetDto vendorBetDto, WinType winType){

        WinDataDto winDataDto = new WinDataDto();
        winDataDto.setExternalTransactionId(Long.toString(vendorBetDto.getBankID()));
        winDataDto.setRoundid(vendorBetDto.getRecordID());
        winDataDto.setGamecode(Integer.toString(vendorBetDto.getGameID()));
        winDataDto.setEventTime(vendorBetDto.getGameDate());
        winDataDto.setWinType(winType);

        //set win amount according bet type
        if(winType == WinType.JACKPOT) {
            winDataDto.setAmount(vendorBetDto.getJpPrize());
            winDataDto.setEffectiveTurnover(BigDecimal.valueOf(0));
        }else{
            winDataDto.setAmount(vendorBetDto.getWin());
            winDataDto.setEffectiveTurnover(vendorBetDto.getBet());
        }


        return winDataDto;

    }
}
