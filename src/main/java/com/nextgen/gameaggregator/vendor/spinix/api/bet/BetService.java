package com.nextgen.gameaggregator.vendor.spinix.api.bet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.vendor.spinix.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.spinix.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.spinix.api.payout.RoundPayoutDto;
import com.nextgen.gameaggregator.vendor.spinix.api.payout.RoundPayoutTransactionDto;
import com.nextgen.gameaggregator.vendor.spinix.constant.TransactionType;
import com.nextgen.gameaggregator.vendor.spinix.service.VendorService;
import com.nextgen.gameaggregator.vendor.spinix.api.payout.RoundPayoutDataVo;
import com.nextgen.gameaggregator.vendor.spinix.api.payout.RoundPayoutDataWalletVo;
import com.nextgen.gameaggregator.vendor.spinix.api.payout.RoundPayoutErrorVo;
import com.nextgen.gameaggregator.vendor.spinix.api.payout.RoundPayoutVo;
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
public class BetService {

    @Autowired
    private HttpService httpService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorService vendorService;

    public RoundPayoutVo doBet(HttpRequestLog httpRequestLog, String traceId, GameSession gameSession, RoundPayoutDto dto, Map<String, RoundPayoutTransactionDto> txnMap, String body) {

        RoundPayoutVo roundPayoutVo = new RoundPayoutVo();
        RoundPayoutDataVo roundPayoutDataVo = new RoundPayoutDataVo();
        RoundPayoutErrorVo roundPayoutErrorVo = new RoundPayoutErrorVo();
        Integer status = HttpStatus.SC_OK;

        try {

            // Set req_id
            roundPayoutVo.setReqId(dto.getReqId());

            // Get bet transaction
            RoundPayoutTransactionDto bet = txnMap.get(TransactionType.BET);

            // Set necessary values to process bet record
            BetDto betDto = new ObjectMapper().convertValue(dto, BetDto.class);
            betDto.setRoundId(dto.getRoundId());
            betDto.setId(bet.getId());
            betDto.setAmount(bet.getAmount().abs());
            betDto.setValidTurnover(dto.getValidTurnover().abs());
            betDto.setGameId(dto.getGameId());
            betDto.setTimestamp(bet.getConvertedTimestamp());

            // process bet
            BetEvent betEvent = walletService.processBet(traceId, gameSession, betDto, body);

            // get balance
            BigDecimal balance = betEvent.getLastBalance();

            // Create RoundPayoutDataWalletVo Object
            RoundPayoutDataWalletVo roundPayoutDataWalletVo = new RoundPayoutDataWalletVo();

            // Set Currency + balance + RoundPayoutDataWalletVo + RoundPayoutDataVo + Status
            roundPayoutDataWalletVo.setBalance(balance);
            roundPayoutDataWalletVo.setCurrency(gameSession.getVendorCurrencyCode());
            roundPayoutDataVo.setWallet(roundPayoutDataWalletVo);
            roundPayoutVo.setData(roundPayoutDataVo);
            roundPayoutVo.setStatus(status);

        } catch (InsufficientBalanceException insufficientBalanceException) {
            roundPayoutErrorVo.setCode(ResponseCodes.INSUFFICIENT_BALANCE);
            roundPayoutVo.setStatus(HttpStatus.SC_BAD_REQUEST);
        } catch (CouchbaseDataIntegrityException couchbaseDataIntegrityException) {
            roundPayoutErrorVo.setCode(ResponseCodes.PARAMETER_INVALID);
            roundPayoutVo.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            roundPayoutErrorVo.setCode(ResponseCodes.USER_NOT_FOUND);
            roundPayoutVo.setStatus(HttpStatus.SC_BAD_REQUEST);
        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            roundPayoutErrorVo.setCode(ResponseCodes.UNEXPECTED_INTERNAL_SERVER_ERROR);
            roundPayoutVo.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            httpService.logError(httpRequestLog, invalidOperatorResponseException);
        } finally {
            if (roundPayoutVo.getStatus() != HttpStatus.SC_OK) {
                roundPayoutErrorVo.setMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(roundPayoutErrorVo.getCode()));
                roundPayoutVo.setError(roundPayoutErrorVo);
            }
        }

        return roundPayoutVo;
    }

    public RoundPayoutVo doWin(HttpRequestLog httpRequestLog, String traceId, GameSession gameSession, RoundPayoutDto dto, Map<String, RoundPayoutTransactionDto> txnMap) {

        RoundPayoutVo roundPayoutVo = new RoundPayoutVo();
        RoundPayoutDataVo roundPayoutDataVo = new RoundPayoutDataVo();
        RoundPayoutErrorVo roundPayoutErrorVo = new RoundPayoutErrorVo();

        try {

            // Set req_id
            roundPayoutVo.setReqId(dto.getReqId());

            // Assign win transaction to a meaningful dto name
            RoundPayoutTransactionDto win = txnMap.get(TransactionType.WIN);

            // Set necessary values to process bet record
            WinDto winDto = new ObjectMapper().convertValue(dto, WinDto.class);
            winDto.setExternalTransactionId(win.getId());
            winDto.setRoundId(dto.getRoundId());
            winDto.setId(win.getId());
            winDto.setAmount(win.getAmount());
            winDto.setFreeSpin(0);
            winDto.setValidTurnover(dto.getValidTurnover().abs());
            winDto.setGameId(dto.getGameId());
            winDto.setTimestamp(win.getConvertedTimestamp());
            winDto.setBetStatus(BetStatus.UNSETTLED);

            // Determine if is free spin
            if (win.getInfo().equals("feature_buy") || win.getInfo().equals("feature_freespin")) {
                winDto.setFreeSpin(1);
            }

            // if win transaction amount has more than 0 means WIN else LOSE
            ResultType resultType = (winDto.getWinAmount().compareTo(BigDecimal.ZERO) > 0) ? ResultType.WIN : ResultType.LOSE;

            if (win.getIsEnd()) {
                winDto.setExternalTransactionId(winDto.getRoundId());
                if (resultType.code == ResultType.LOSE.code) {
                    // if result type is LOSE and bet status is SETTLED, change to END
                    resultType = ResultType.END;
                }
                winDto.setBetStatus(BetStatus.SETTLED);
            }

            BigDecimal balance = walletService.processBetResult(traceId, gameSession, winDto, resultType, vendorService, httpRequestLog);

            // Create RoundPayoutDataWalletVo Object
            RoundPayoutDataWalletVo roundPayoutDataWalletVo = new RoundPayoutDataWalletVo();

            // Set Currency + balance + RoundPayoutDataWalletVo + Status
            roundPayoutDataWalletVo.setCurrency(gameSession.getVendorCurrencyCode());
            roundPayoutDataWalletVo.setBalance(balance);
            roundPayoutDataVo.setWallet(roundPayoutDataWalletVo);
            roundPayoutVo.setStatus(HttpStatus.SC_OK);

        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            roundPayoutErrorVo.setCode(ResponseCodes.USER_NOT_FOUND);
            roundPayoutVo.setStatus(HttpStatus.SC_BAD_REQUEST);
        } catch (BetResultIdempotentViolationException | MergedBetDataIntegrityException parameterInvalidException) {
            roundPayoutErrorVo.setCode(ResponseCodes.PARAMETER_INVALID);
            roundPayoutVo.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        } catch (InsufficientBalanceException insufficientBalanceException) {
            roundPayoutErrorVo.setCode(ResponseCodes.INSUFFICIENT_BALANCE);
            roundPayoutVo.setStatus(HttpStatus.SC_BAD_REQUEST);
        } catch (BetNotFoundException betNotFoundException) {
            roundPayoutVo = vendorService.getCurrentBalanceResponseVo(httpRequestLog, traceId, gameSession, dto);
        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            roundPayoutErrorVo.setCode(ResponseCodes.UNEXPECTED_INTERNAL_SERVER_ERROR);
            roundPayoutVo.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            httpService.logError(httpRequestLog, invalidOperatorResponseException);
        } catch (Exception exception) {
            // TODO: catch IdempotentException
            roundPayoutErrorVo.setCode(ResponseCodes.UNEXPECTED_INTERNAL_SERVER_ERROR);
            roundPayoutVo.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            httpService.logError(httpRequestLog, exception);
        } finally {
            if (roundPayoutVo.getStatus() == HttpStatus.SC_OK) {
                roundPayoutVo.setData(roundPayoutDataVo);
            } else {
                roundPayoutErrorVo.setMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(roundPayoutErrorVo.getCode()));
                roundPayoutVo.setError(roundPayoutErrorVo);
            }
        }

        return roundPayoutVo;
    }

    public RoundPayoutVo doBetAndWin(HttpRequestLog httpRequestLog, String traceId, GameSession gameSession, RoundPayoutDto dto, Map<String, RoundPayoutTransactionDto> txnMap) {

        RoundPayoutVo roundPayoutVo = new RoundPayoutVo();
        RoundPayoutDataVo roundPayoutDataVo = new RoundPayoutDataVo();
        RoundPayoutErrorVo roundPayoutErrorVo = new RoundPayoutErrorVo();

        try {

            // Set req_id
            roundPayoutVo.setReqId(dto.getReqId());

            // Prepare variables for RoundPayoutTransactionDto bet and win
            RoundPayoutTransactionDto bet = txnMap.get(TransactionType.BET);
            RoundPayoutTransactionDto win = txnMap.get(TransactionType.WIN);

            BetWinDto betWinDto = new BetWinDto();
            betWinDto.setReqId(dto.getReqId());
            betWinDto.setExternalTransactionId(dto.getRoundId());
            betWinDto.setRoundId(dto.getRoundId());
            betWinDto.setId(bet.getId());
            betWinDto.setBetAmount(bet.getAmount().abs());
            betWinDto.setWinAmount(win.getAmount());
            betWinDto.setTimestamp(bet.getConvertedTimestamp());
            betWinDto.setFreeSpin(0);
            betWinDto.setGameId(dto.getGameId());
            betWinDto.setValidTurnover(bet.getAmount().abs());
            betWinDto.setWinLossAmount(win.getAmount().subtract(bet.getAmount()));
            betWinDto.setBetStatus(BetStatus.UNSETTLED);

            // Determine if bet is settled
            if (win.getIsEnd()) {
                betWinDto.setBetStatus(BetStatus.SETTLED);
            }

            // Determine if is free spin
            if (win.getInfo().equals("feature_buy") || win.getInfo().equals("feature_freespin")) {
                betWinDto.setFreeSpin(1);
            }

            // Get result type
            ResultType resultType = vendorService.calculateResultType(betWinDto.getBetAmount(), betWinDto.getWinAmount(), BigDecimal.ZERO, true);

            BigDecimal balance = walletService.processBetResult(traceId, gameSession, betWinDto, resultType, vendorService, httpRequestLog);

            // Create RoundPayoutDataWalletVo Object
            RoundPayoutDataWalletVo roundPayoutDataWalletVo = new RoundPayoutDataWalletVo();

            // Set Currency + balance + RoundPayoutDataWalletVo + Status
            roundPayoutDataWalletVo.setCurrency(gameSession.getVendorCurrencyCode());
            roundPayoutDataWalletVo.setBalance(balance);
            roundPayoutDataVo.setWallet(roundPayoutDataWalletVo);
            roundPayoutVo.setStatus(HttpStatus.SC_OK);

        } catch (InvalidAgentApiCredentialException invalidAgentApiCredentialException) {
            roundPayoutErrorVo.setCode(ResponseCodes.USER_NOT_FOUND);
            roundPayoutVo.setStatus(HttpStatus.SC_BAD_REQUEST);
        } catch (MergedBetDataIntegrityException |
                 BetResultIdempotentViolationException parameterInvalidException) {
            roundPayoutErrorVo.setCode(ResponseCodes.PARAMETER_INVALID);
            roundPayoutVo.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        } catch (InsufficientBalanceException insufficientBalanceException) {
            roundPayoutErrorVo.setCode(ResponseCodes.INSUFFICIENT_BALANCE);
            roundPayoutVo.setStatus(HttpStatus.SC_BAD_REQUEST);
        } catch (BetNotFoundException betNotFoundException) {
            roundPayoutVo = vendorService.getCurrentBalanceResponseVo(httpRequestLog, traceId, gameSession, dto);
        } catch (InvalidOperatorResponseException invalidOperatorResponseException) {
            if (invalidOperatorResponseException.getOperatorStatus() != null && invalidOperatorResponseException.getOperatorStatus() == com.nextgen.gameaggregator.operator.constant.ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code) {
                roundPayoutErrorVo.setCode(ResponseCodes.INSUFFICIENT_BALANCE);
                roundPayoutVo.setStatus(HttpStatus.SC_BAD_REQUEST);
            } else {
                roundPayoutErrorVo.setCode(ResponseCodes.UNEXPECTED_INTERNAL_SERVER_ERROR);
                roundPayoutVo.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                httpService.logError(httpRequestLog, invalidOperatorResponseException);
            }
        } catch (Exception exception) {
            roundPayoutErrorVo.setCode(ResponseCodes.UNEXPECTED_INTERNAL_SERVER_ERROR);
            roundPayoutVo.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            httpService.logError(httpRequestLog, exception);
        } finally {
            if (roundPayoutVo.getStatus() == HttpStatus.SC_OK) {
                roundPayoutVo.setData(roundPayoutDataVo);
            } else {
                roundPayoutErrorVo.setMessage(ResponseCodes.RESPONSE_DESCRIPTION.get(roundPayoutErrorVo.getCode()));
                roundPayoutVo.setError(roundPayoutErrorVo);
            }
        }

        return roundPayoutVo;
    }

}