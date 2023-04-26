package com.nextgen.gameaggregator.vendor.spadegaming.api.transfer;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetRollbackEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.*;
import com.nextgen.gameaggregator.vendor.spadegaming.service.VendorService;

import jakarta.servlet.http.HttpServletRequest;

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

    @Autowired
    private VendorService vendorService;

    public TransferVo transfer(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String body = httpRequestLog.getRequestBody();
        String traceId = httpRequestLog.getTraceId();
        TransferVo transferVo = new TransferVo();

        try {
            TransferDto dto = HttpService.convertJsonToDto(body, TransferDto.class);
            transferVo.setMerchantCode(dto.getMerchantCode());
            transferVo.setSerialNo(traceId);
            this.doValidation(dto);

            // User acctId and gameCode to get rawGameSession if gameCode is not null
            GameSession gameSession = dto.getGameCode() != null
                    ? gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(dto.getAcctId(), dto.getGameCode())
                    : gameSessionService.getGameSessionByVendorPlayerUsername(dto.getAcctId());

            String merchantCode = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.MERCHANT_CODE);
            this.doVerification(dto, gameSession, merchantCode);

            switch (dto.getType()) {
                case Actions.PLACE_BET -> {
                    // Place bet action
                    BigDecimal balance = walletService.processBet(traceId, gameSession, dto, body);
                    transferVo.setBalance(balance);
                    transferVo.setMsg(ResponseCode.SUCCESS.description);
                    transferVo.setResponseCode(ResponseCode.SUCCESS);
                }
                case Actions.CANCEL_BET -> {
                    // Cancel bet and refund action
                    BigDecimal rollbackBalance = walletService.processRollback(traceId, dto, gameSession);
                    transferVo.setBalance(rollbackBalance);
                    transferVo.setMsg(ResponseCode.SUCCESS.description);
                    transferVo.setResponseCode(ResponseCode.SUCCESS);
                }
                case Actions.PAYOUT -> {
                    // Payout action
                    WinDataDto winDataDto = new ObjectMapper().convertValue(dto, WinDataDto.class);
                    String type = Optional.ofNullable(dto.getSpecialGame()) // Check type in SpecialGame
                            .map(SpecialGameDto::getType) // Map with dto
                            .orElse(null);
                    BigDecimal payoutBalance = (type != null && type.equals("Free")) // If free spin, use BET_WIN
                            ? walletService.processBetResult(traceId, gameSession, winDataDto, ResultType.BET_WIN, vendorService, body)
                            : walletService.processBetResult(traceId, gameSession, winDataDto, // Else determine WIN or LOSE
                            (winDataDto.getAmount().compareTo(BigDecimal.ZERO) > 0) ? ResultType.WIN : ResultType.LOSE, vendorService, body);
                    transferVo.setBalance(payoutBalance);
                    transferVo.setMsg(ResponseCode.SUCCESS.description);
                    transferVo.setResponseCode(ResponseCode.SUCCESS);
                }
                case Actions.BONUS -> transferVo.setResponseCode(ResponseCode.SUCCESS);
                default -> transferVo.setResponseCode(ResponseCode.INVALID_REQUEST);
            }

            transferVo.setTransferId(dto.getTransferId());
            transferVo.setMerchantCode(dto.getMerchantCode());
            transferVo.setMerchantTxId(gameSession.getToken());
            transferVo.setAcctId(gameSession.getVendorPlayerUsername());
            transferVo.setSerialNo(traceId);

        } catch (InvalidRequestException |
                 GameNotSupportedException |
                 InvalidAgentApiCredentialException |
                 InvalidOperatorResponseException invalidException) {
            transferVo.setResponseCode(ResponseCode.INVALID_REQUEST);

        } catch (DisabledVendorLineException disabledVendorLineException) {
            transferVo.setResponseCode(ResponseCode.SERVICE_INACCESSIBLE);

        } catch (DisabledAgentPlayerException disabledAgentPlayerException) {
            transferVo.setResponseCode(ResponseCode.SERVICE_INACCESSIBLE);

        } catch (DisabledGameException disabledGameException) {
            transferVo.setResponseCode(ResponseCode.SERVICE_INACCESSIBLE);

        } catch (CurrencyNotSupportedException currencyNotSupportedException) {
            transferVo.setResponseCode(ResponseCode.CURRENCY_INVALID);

        } catch (JsonProcessingException jsonProcessingException) {
            transferVo.setResponseCode(ResponseCode.INVALID_FORMAT);

        } catch (AuthenticationException authenticationException) {
            transferVo.setResponseCode(ResponseCode.ACCT_NOT_FOUND);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            transferVo.setResponseCode(ResponseCode.INSUFFICIENT_BALANCE);

        } catch (BetNotFoundException betNotFoundException) {
            transferVo.setResponseCode(ResponseCode.RECORD_ID_NOT_FOUND);

        } catch (MergedBetDataIntegrityException mergedBetDataIntegrityException) {
            transferVo.setResponseCode(ResponseCode.SERVICE_INACCESSIBLE);

        } catch (RecordNotFoundException recordNotFoundException) {
            transferVo.setResponseCode(ResponseCode.RECORD_ID_NOT_FOUND);

        } catch (UnableToFindCredentialsException unableToFindCredentialsException) {
            transferVo.setResponseCode(ResponseCode.MERCHANT_NOT_FOUND);

        } catch (CredentialNotFoundException credentialNotFoundException) {
            transferVo.setResponseCode(ResponseCode.MERCHANT_NOT_FOUND);

        } catch (CouchbaseDataIntegrityException couchbaseDataIntegrityException) {
            transferVo.setResponseCode(ResponseCode.SERVICE_INACCESSIBLE);

        } finally {
            httpService.end(httpRequestLog, transferVo);
        }

        return transferVo;
    }

    private void doValidation(TransferDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }

    private void doVerification(TransferDto dto, GameSession gameSession, String merchantCode)
            throws
            UnableToFindCredentialsException,
            AuthenticationException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            GameNotSupportedException,
            CurrencyNotSupportedException,
            InvalidRequestException {

        // Verify received merchant code is same from Credentials merchant code 
        ValidationUtils.isEquals(merchantCode, dto.getMerchantCode(), UnableToFindCredentialsException::new);

        // Verify received vendor player username is the same from game session
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

        // Verify channel
        if (!Channel.list.contains(dto.getChannel())) throw new InvalidRequestException();

    }
}
