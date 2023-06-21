package com.nextgen.gameaggregator.vendor.spinix.api.cancel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.spinix.api.payout.RoundPayoutVo;
import com.nextgen.gameaggregator.vendor.spinix.api.payout.RoundPayoutDataVo;
import com.nextgen.gameaggregator.vendor.spinix.api.payout.RoundPayoutDataWalletVo;
import com.nextgen.gameaggregator.vendor.spinix.api.payout.RoundPayoutErrorVo;
import com.nextgen.gameaggregator.vendor.spinix.api.payout.RoundPayoutDto;
import com.nextgen.gameaggregator.vendor.spinix.api.payout.RoundPayoutTransactionDto;
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
    private VendorPlayerService vendorPlayerService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private BetHistoryService betHistoryService;

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
            BigDecimal balance = walletService.processRollback(traceId, dto, gameSession, vendorService);

            // Set Balance
            roundPayoutDataWalletVo.setBalance(balance);

            // Set Currency + RoundPayoutDataWalletVo + roundPayoutDataVo + Status
            roundPayoutDataWalletVo.setCurrency(gameSession.getVendorCurrencyCode());
            roundPayoutDataVo.setWallet(roundPayoutDataWalletVo);
            roundPayoutVo.setData(roundPayoutDataVo);
            roundPayoutVo.setStatus(status);

        } catch (BetNotFoundException betNotFoundException) {
            roundPayoutVo = vendorService.getCurrentBalanceResponseVo(httpRequestLog, traceId, gameSession, dto);
        } catch (BetRefundIdempotentViolationException | TransactionInvalidException transactionInvalidException) {
            roundPayoutErrorVo.setCode(ResponseCodes.TRANSACTION_INVALID);
            roundPayoutVo.setStatus(HttpStatus.SC_BAD_REQUEST);
        } catch (GameNotSupportedException gameNotSupportedException) {
            roundPayoutErrorVo.setCode(ResponseCodes.GAME_NOT_FOUND);
            roundPayoutVo.setStatus(HttpStatus.SC_BAD_REQUEST);
        } catch (JsonProcessingException | CouchbaseDataIntegrityException parameterInvalidException) {
            roundPayoutErrorVo.setCode(ResponseCodes.PARAMETER_INVALID);
            roundPayoutVo.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        } catch (RecordNotFoundException | InvalidPlayerException | InvalidAgentApiCredentialException |
                 InvalidOperatorResponseException userNotFoundException) {
            roundPayoutErrorVo.setCode(ResponseCodes.USER_NOT_FOUND);
            roundPayoutVo.setStatus(HttpStatus.SC_BAD_REQUEST);
            httpService.logError(httpRequestLog, userNotFoundException);
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
            throws InvalidPlayerException, GameNotSupportedException, BetNotFoundException,
            CouchbaseDataIntegrityException, JsonProcessingException, TransactionInvalidException {

        // Gather require data
        VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(gameSession.getVendorPlayerUsername());
        VendorGame vendorGame = vendorGameService.getByVendorGameCodeAndVendorId(gameSession.getVendorGameCode(), vendorPlayer.getVendorId());
        UnsettledBet unsettledBet = betHistoryService.getRawUnsettledBetByBetIdAndRoundIdAndGameIdAndPlayerId(dto.getRoundId(),
                dto.getRoundId(), vendorGame.getId(), vendorPlayer.getId());

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