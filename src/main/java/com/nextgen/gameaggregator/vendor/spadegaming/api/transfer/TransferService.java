package com.nextgen.gameaggregator.vendor.spadegaming.api.transfer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.Actions;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.Channel;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.Credentials;
import com.nextgen.gameaggregator.vendor.spadegaming.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.spadegaming.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class TransferService {
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private ValidationService validationService;

    public TransferVo transfer(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String body = httpRequestLog.getRequestBody();
        String traceId = httpRequestLog.getId();
        TransferVo transferVo = new TransferVo();
        String transferId = "";
        String merchantCode = "";
        String merchantTxId = "";
        String acctId = "";
        Boolean isCancel = false;

        try {
            TransferDto dto = HttpService.convertJsonToDto(body, TransferDto.class);
            transferVo.setMerchantCode(dto.getMerchantCode());
            transferVo.setSerialNo(traceId);
            this.doValidation(dto);

            // User acctId and gameCode to get rawGameSession if gameCode is not null
            GameSession gameSession = dto.getGameCode() != null
                    ? gameSessionService.getGameSessionByVendorPlayerUsernameAndVendorGameCode(dto.getAcctId(), dto.getGameCode())
                    : gameSessionService.getGameSessionByVendorPlayerUsername(dto.getAcctId());

            merchantCode = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.MERCHANT_CODE);
            transferId = dto.getTransferId();
            merchantTxId = gameSession.getToken();
            acctId = gameSession.getVendorPlayerUsername();
            this.doVerification(dto, gameSession, merchantCode);

            switch (dto.getType()) {
                case Actions.PLACE_BET -> {
                    // Place bet action
                    BetEvent betEvent = walletService.processBet(traceId, gameSession, dto, body, httpRequestLog);
                    transferVo.setBalance(betEvent.getLastBalance());
                    transferVo.setMsg(ResponseCode.SUCCESS.description);
                    transferVo.setResponseCode(ResponseCode.SUCCESS);
                }
                case Actions.CANCEL_BET -> {
                    // Cancel bet and refund action
                    isCancel = true;
                    BigDecimal rollbackBalance = walletService.processRollback(traceId, dto, gameSession, vendorService, httpRequestLog);
                    transferVo.setBalance(rollbackBalance);
                    transferVo.setMsg(ResponseCode.SUCCESS.description);
                    transferVo.setResponseCode(ResponseCode.SUCCESS);
                }
                case Actions.PAYOUT -> {
                    // Payout action
                    WinDataDto winDataDto = new ObjectMapper().convertValue(dto, WinDataDto.class);
                    String type = Optional.ofNullable(dto.getSpecialGame()).map(SpecialGameDto::getType).orElse(null);
                    ResultType resultType = determineResultType(type, winDataDto);
                    BigDecimal payoutBalance = walletService.processBetResult(traceId, gameSession, winDataDto, resultType, vendorService, httpRequestLog);
                    transferVo.setBalance(payoutBalance);
                    transferVo.setMsg(ResponseCode.SUCCESS.description);
                    transferVo.setResponseCode(ResponseCode.SUCCESS);
                }
                default -> transferVo.setResponseCode(ResponseCode.INVALID_REQUEST);
            }

            transferVo.setTransferId(transferId);
            transferVo.setMerchantCode(merchantCode);
            transferVo.setMerchantTxId(merchantTxId);
            transferVo.setAcctId(acctId);
            transferVo.setSerialNo(traceId);

        } catch (AuthenticationException authenticationException) {
            // account not found 
            httpService.logError(httpRequestLog, authenticationException);
            transferVo.setResponseCode(ResponseCode.ACCT_NOT_FOUND);

        } catch (CredentialNotFoundException | UnableToFindCredentialsException |
                 InvalidPlayerException merchantNotFoundException) {
            // merchant not found
            httpService.logError(httpRequestLog, merchantNotFoundException);
            transferVo.setResponseCode(isCancel ? ResponseCode.INVALID_REQUEST : ResponseCode.SYSTEM_ERROR);

        } catch (DisabledVendorLineException | DisabledAgentPlayerException |
                 DisabledGameException | MergedBetDataIntegrityException serviceInaccessibleException) {
            // service inaccessible 
            httpService.logError(httpRequestLog, serviceInaccessibleException);
            transferVo.setResponseCode(ResponseCode.SERVICE_INACCESSIBLE);

        } catch (CurrencyNotSupportedException currencyInvalidException) {
            // invalid currency
            httpService.logError(httpRequestLog, currencyInvalidException);
            transferVo.setResponseCode(ResponseCode.CURRENCY_INVALID);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            // insufficient balance
            httpService.logError(httpRequestLog, insufficientBalanceException);
            transferVo.setResponseCode(ResponseCode.INSUFFICIENT_BALANCE);

        } catch (BetNotFoundException | RecordNotFoundException notFoundException) {
            // record ID not found
            httpService.logError(httpRequestLog, notFoundException);
            transferVo.setResponseCode(isCancel ? ResponseCode.RELATED_ID_NOT_FOUND : ResponseCode.RECORD_ID_NOT_FOUND);

        } catch (InvalidRequestException | InvalidOperatorResponseException |
                 InvalidAgentApiCredentialException | GameNotSupportedException |
                 BetRefundIdempotentViolationException invalidRequestException) {
            // invalid request
            httpService.logError(httpRequestLog, invalidRequestException);
            transferVo.setResponseCode(ResponseCode.INVALID_REQUEST);

        } catch (JsonProcessingException invalidFormatException) {
            // invalid format
            httpService.logError(httpRequestLog, invalidFormatException);
            transferVo.setResponseCode(ResponseCode.INVALID_FORMAT);

        } catch (BetResultIdempotentViolationException e) {
            transferVo.setBalance(e.getBalance());
            transferVo.setMsg(ResponseCode.SUCCESS.description);
            transferVo.setResponseCode(ResponseCode.SUCCESS);
            transferVo.setTransferId(transferId);
            transferVo.setMerchantCode(merchantCode);
            transferVo.setMerchantTxId(merchantTxId);
            transferVo.setAcctId(acctId);
            transferVo.setSerialNo(traceId);
        } catch (Exception exception) {
            transferVo.setResponseCode(isCancel ? ResponseCode.INVALID_REQUEST : ResponseCode.SYSTEM_ERROR);
            httpService.logError(httpRequestLog, exception);

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
            InvalidRequestException, InvalidPlayerException {

        // Verify received merchant code is same from Credentials merchant code 
        ValidationUtils.isEquals(merchantCode, dto.getMerchantCode(), UnableToFindCredentialsException::new);

        // Verify received vendor player username is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getAcctId(), AuthenticationException::new);

        // Verify vendor gameCode and currency
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), String.valueOf(dto.getGameCode()), GameNotSupportedException::new);
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), dto.getCurrency(), CurrencyNotSupportedException::new);

        //validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, dto.getAcctId());

        // Verify channel
        if (!Channel.list.contains(dto.getChannel())) throw new InvalidRequestException();
    }

    private ResultType determineResultType(String type, WinDataDto winDataDto) {
        BigDecimal amount = winDataDto.getAmount();
        boolean isSpecialType = type != null;

        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            if (isSpecialType && winDataDto.getSpecialGame().getSequence() == 0) {
                return ResultType.WIN;

            } else {
                return isSpecialType ? ResultType.BET_WIN : ResultType.WIN;

            }

        } else {
            return isSpecialType ? ResultType.BET_LOSE : ResultType.END;
        }
    }
}
