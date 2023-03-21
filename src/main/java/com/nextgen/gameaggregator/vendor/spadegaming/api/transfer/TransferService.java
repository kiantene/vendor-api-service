package com.nextgen.gameaggregator.vendor.spadegaming.api.transfer;

import java.math.BigDecimal;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.enums.WinType;
import com.nextgen.gameaggregator.eventing.core.EventDispatcherSystem;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.eventing.events.BetResultEvent;
import com.nextgen.gameaggregator.eventing.events.EndRoundEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.ResponseCode;

@Service
public class TransferService {
    @Autowired
    private HttpService httpService;

    @Autowired
    private VendorLineService vendorLineService;

    @Autowired
    private AgentPlayerService agentPlayerService;

    @Autowired
    private VendorGameService vendorGameService;

    @Autowired
    private GameSessionService gameSessionService;

    @Autowired
    private WalletService walletService;
    
    public TransferVo transfer(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String body = httpRequestLog.getRequestBody();
        String traceId = httpRequestLog.getTraceId();
        TransferVo transferVo = new TransferVo();
        transferVo.setMerchantCode(Credentials.MERCHANT_CODE);
        transferVo.setSerialNo(traceId);
    
        try {
            TransferDto dto = HttpService.convertJsonToDto(body, TransferDto.class);
            this.doValidation(dto);

            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getAcctId());
            this.doVerification(dto, gameSession);
    
            switch(dto.getType()) {
                case 1:
                    // Place bet action
                    BetEvent betEvent = walletService.processBet(traceId, gameSession, dto, body);
                    transferVo.setBalance(betEvent.getLastBalance());
                    transferVo.setMsg(ResponseCode.SUCCESS.description);
                    transferVo.setResponseCode(ResponseCode.SUCCESS);
                    break;
                case 2:
                    // Cancel bet action
                    BigDecimal balance = walletService.getBalance(traceId, gameSession);
                    transferVo.setBalance(balance);
                    transferVo.setMsg(ResponseCode.SUCCESS.description);
                    transferVo.setResponseCode(ResponseCode.SUCCESS);
                    break;
                case 4:
                    // Payout action
                    WinDataDto winDataDto = new ObjectMapper().convertValue(dto, WinDataDto.class);
                    winDataDto.setExternalTransactionId(dto.getTransferId());
                    winDataDto.setRoundId(dto.getReferenceId());
                    winDataDto.setAmount(dto.getAmount());
                    winDataDto.setEffectiveTurnover(dto.getAmount());
                    winDataDto.setGameId(dto.getGameCode());
                    winDataDto.setWinType(getWinType(dto));

                    BetResultEvent betResultEvent = walletService.processWin(traceId, gameSession, winDataDto, body);
                    transferVo.setBalance(betResultEvent.getLastBalance());
                    transferVo.setMsg(ResponseCode.SUCCESS.description);
                    transferVo.setResponseCode(ResponseCode.SUCCESS);

                    // Emit event for additional asynchronous processing
                    EventDispatcherSystem.emitAsync(new EndRoundEvent(betResultEvent.getBetHistory()));

                    break;
                default:
                    transferVo.setResponseCode(ResponseCode.INVALID_REQUEST);
                    break;
            }
    
            transferVo.setTransferId(dto.getTransferId());
            transferVo.setMerchantCode(Credentials.MERCHANT_CODE);
            transferVo.setMerchantTxId(gameSession.getToken());
            transferVo.setAcctId(gameSession.getVendorPlayerUsername());
            transferVo.setSerialNo(traceId);
        
        } catch (InvalidRequestException invalidRequestException) {
            transferVo.setResponseCode(ResponseCode.INVALID_REQUEST);
        
        } catch (DisabledVendorLineException disabledVendorLineException) {
            transferVo.setResponseCode(ResponseCode.SERVICE_INACCESSIBLE);

        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            transferVo.setResponseCode(ResponseCode.SERVICE_INACCESSIBLE);

        } catch (DisabledGameException disabledGameException) {
            transferVo.setResponseCode(ResponseCode.SERVICE_INACCESSIBLE);
        
        } catch (GameNotSupportedException gameNotSupportedException) {
            transferVo.setResponseCode(ResponseCode.INVALID_REQUEST);

        } catch (CurrencyNotSupportedException currencyNotSupportedException) {
            transferVo.setResponseCode(ResponseCode.CURRENCY_INVALID);

        } catch (JsonProcessingException jsonProcessingException) {
            transferVo.setResponseCode(ResponseCode.INVALID_FORMAT);

        } catch (AuthenticationException authenticationException) {
            transferVo.setResponseCode(ResponseCode.TOKEN_VALIDATION_FAILED);

        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            transferVo.setResponseCode(ResponseCode.INVALID_REQUEST);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            transferVo.setResponseCode(ResponseCode.INSUFFICIENT_BALANCE);

        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            transferVo.setResponseCode(ResponseCode.INVALID_REQUEST);

        } catch (DuplicateExternalTransactionIdException duplicateExternalTransactionIdException) {
            transferVo.setResponseCode(ResponseCode.DUPLICATED_REQUEST);

        } catch (BetNotFoundException betNotFoundException) {
            transferVo.setResponseCode(ResponseCode.RECORD_ID_NOT_FOUND);

        } catch (BetResultNotFoundException betResultNotFoundException) {
            transferVo.setResponseCode(ResponseCode.RECORD_ID_NOT_FOUND);

        } finally {
            httpService.end(httpRequestLog, transferVo);
        }
    
        return transferVo;
    }

    private WinType getWinType(TransferDto dto) {
        return (dto.getAmount().compareTo(BigDecimal.ZERO) > 0) ? WinType.WIN : WinType.LOSE;
    }

    private void doValidation(TransferDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(TransferDto dto, GameSession gameSession)
            throws
            AuthenticationException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            GameNotSupportedException,
            CurrencyNotSupportedException {

        // Verify received vendor player username is the same from game session
        // Comparison for game session value will always be using AuthenticationException
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getAcctId(), AuthenticationException::new);

        // Verify vendor gameCode and currency
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(dto.getGameCode()), GameNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);

        // Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        // Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        // Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());

    }
}
