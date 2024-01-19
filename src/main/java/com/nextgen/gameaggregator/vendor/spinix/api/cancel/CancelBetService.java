package com.nextgen.gameaggregator.vendor.spinix.api.cancel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.entity.ga.UnsettledBet;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.spinix.api.payout.*;
import com.nextgen.gameaggregator.vendor.spinix.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.spinix.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.spinix.constant.TransactionType;
import com.nextgen.gameaggregator.vendor.spinix.exception.TransactionInvalidException;
import com.nextgen.gameaggregator.vendor.spinix.service.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class CancelBetService {

    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private SettledBetService settledBetService;
    @Autowired
    private UnsettledBetService unsettledBetService;

    public RoundPayoutVo cancelBet(HttpRequestLog httpRequestLog, String traceId, GameSession gameSession, RoundPayoutDto dto) {

        RoundPayoutVo roundPayoutVo = new RoundPayoutVo();
        RoundPayoutDataVo roundPayoutDataVo = new RoundPayoutDataVo();
        RoundPayoutDataWalletVo roundPayoutDataWalletVo = new RoundPayoutDataWalletVo();
        RoundPayoutErrorVo roundPayoutErrorVo = new RoundPayoutErrorVo();
        Integer status = HttpStatus.SC_OK;

        try {

            // Get and set req id
            String reqId = dto.getReqId();
            roundPayoutVo.setReqId(reqId);

            // Verify cancel bet data
            this.verifyCancelBet(gameSession, dto);

            // Verify if there is a win transaction within unsettled bet
            this.verifyUnsettledWinTransaction(gameSession, dto);

            // Send refund to Operator
            BigDecimal balance = walletService.processRollback(traceId, dto, gameSession, vendorService, httpRequestLog);

            // Set Balance
            roundPayoutDataWalletVo.setBalance(balance);

            // Set Currency + RoundPayoutDataWalletVo + roundPayoutDataVo + Status
            roundPayoutDataWalletVo.setCurrency(gameSession.getVendorCurrencyCode());
            roundPayoutDataVo.setWallet(roundPayoutDataWalletVo);
            roundPayoutVo.setData(roundPayoutDataVo);
            roundPayoutVo.setStatus(status);

        } catch (BetRefundIdempotentViolationException betRefundIdempotentViolationException) {
            roundPayoutVo = vendorService.getCurrentBalanceResponseVo(httpRequestLog, traceId, gameSession, dto);
        } catch (TransactionInvalidException | BetNotFoundException transactionInvalidException) {
            roundPayoutErrorVo.setCode(ResponseCodes.TRANSACTION_INVALID);
            roundPayoutVo.setStatus(HttpStatus.SC_BAD_REQUEST);
        } catch (GameNotSupportedException gameNotSupportedException) {
            roundPayoutErrorVo.setCode(ResponseCodes.GAME_NOT_FOUND);
            roundPayoutVo.setStatus(HttpStatus.SC_BAD_REQUEST);
        } catch (JsonProcessingException parameterInvalidException) {
            roundPayoutErrorVo.setCode(ResponseCodes.PARAMETER_INVALID);
            roundPayoutVo.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        } catch (RecordNotFoundException | InvalidPlayerException | InvalidAgentApiCredentialException userNotFoundException) {
            roundPayoutErrorVo.setCode(ResponseCodes.USER_NOT_FOUND);
            roundPayoutVo.setStatus(HttpStatus.SC_BAD_REQUEST);
        } catch (TransactionStillProcessingException transactionStillProcessingException) {
            roundPayoutErrorVo.setCode(ResponseCodes.UNEXPECTED_INTERNAL_SERVER_ERROR);
            roundPayoutVo.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        } catch (BetResultIdempotentViolationException betResultIdempotentViolationException) {
            if (betResultIdempotentViolationException.getStatus() == BetStatus.REFUNDED.code) {
                // if bet already refunded
                roundPayoutVo = vendorService.getCurrentBalanceResponseVo(httpRequestLog, traceId, gameSession, dto);
            } else {
                // if found the bet other in settled status (cancel / unsettle / settled)
                roundPayoutErrorVo.setCode(ResponseCodes.TRANSACTION_INVALID);
                roundPayoutVo.setStatus(HttpStatus.SC_BAD_REQUEST);
            }
        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            roundPayoutErrorVo.setCode(ResponseCodes.UNEXPECTED_INTERNAL_SERVER_ERROR);
            roundPayoutVo.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            httpService.logError(httpRequestLog, invalidOperatorResponseException);
        } catch (Exception exception) {
            roundPayoutErrorVo.setCode(ResponseCodes.UNEXPECTED_INTERNAL_SERVER_ERROR);
            roundPayoutVo.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            httpService.logError(httpRequestLog, exception);
        } finally {
            if (roundPayoutVo.getStatus() != HttpStatus.SC_OK) {
                roundPayoutErrorVo.setMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(roundPayoutErrorVo.getCode()));
                roundPayoutVo.setError(roundPayoutErrorVo);
            }
        }

        return roundPayoutVo;
    }

    /*****
     *
     * @param gameSession
     * @param dto
     * @throws TransactionInvalidException
     * @throws BetRefundIdempotentViolationException
     * This will allow the following test cases to pass
     * 1. If settled bet is found and already refunded, will throw BetRefundIdempotentViolationException and return success response
     * 2. If settled bet failed and vendor send cancel bet request, we must return transaction invalid error
     */
    private void verifyCancelBet(GameSession gameSession, RoundPayoutDto dto) throws TransactionInvalidException, BetRefundIdempotentViolationException {
        try {
            SettledBet settledBet = settledBetService.getByVendorPlayerIdAndExternalTransactionId(gameSession.getVendorPlayerId(), dto.getRoundId());

            if (settledBet != null && settledBet.getStatus() == BetStatus.REFUNDED.code) {
                // if refunded already, return success
                throw new BetRefundIdempotentViolationException();
            }

            // if has not refunded before and bet already settled throw error
            throw new TransactionInvalidException();

        } catch (BetNotFoundException e) {
            // does nothing
        }
    }

    private void verifyUnsettledWinTransaction(GameSession gameSession, RoundPayoutDto dto)
            throws InvalidPlayerException, GameNotSupportedException, BetNotFoundException, JsonProcessingException, TransactionInvalidException {

        UnsettledBet unsettledBet = unsettledBetService.getUnsettledBetByRoundId(dto.getRoundId(), dto.getRoundId(), gameSession.getVendorGameId(), gameSession.getVendorPlayerId());

        String data = unsettledBet.getRawData();
        RoundPayoutDto unsettledBetDto = HttpService.convertJsonToDto(data, RoundPayoutDto.class);
        Map<String, RoundPayoutTransactionDto> txnMap = vendorService.getTransactions(unsettledBetDto);

        // Check for any win transaction in unsettled bet couchbase
        RoundPayoutTransactionDto unsettledWin = txnMap.get(TransactionType.WIN);

        // Throw transaction invalid exception if there is a win transaction
        if (unsettledWin != null) {
            throw new TransactionInvalidException();
        }

    }

}