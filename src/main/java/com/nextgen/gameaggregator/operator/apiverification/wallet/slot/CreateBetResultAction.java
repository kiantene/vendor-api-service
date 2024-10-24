package com.nextgen.gameaggregator.operator.apiverification.wallet.slot;

import com.nextgen.gameaggregator.entity.ga.BetHistory;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.RawBetIdempotentLog;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.exception.BetNotFoundException;
import com.nextgen.gameaggregator.exception.BetResultIdempotentViolationException;
import com.nextgen.gameaggregator.operator.apiverification.wallet.slot.dto.WalletCreateBetResultDto;
import com.nextgen.gameaggregator.operator.constant.EndPoints;
import com.nextgen.gameaggregator.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping(path = EndPoints.API_VERIFY_PATH)
@Slf4j
public class CreateBetResultAction {
    private final RequestService requestService;
    private final Boolean isTestEnv;
    private final HttpService httpService;
    private final SettledBetService settledBetService;
    private final BetIdempotentLogService betIdempotentLogService;
    private final KafkaService kafkaService;

    public CreateBetResultAction(
            RequestService requestService,
            @Value("${is-test-env:false}") Boolean isTestEnv,
            HttpService httpService, SettledBetService settledBetService, BetIdempotentLogService betIdempotentLogService, KafkaService kafkaService) {
        this.requestService = requestService;
        this.isTestEnv = isTestEnv;
        this.httpService = httpService;
        this.settledBetService = settledBetService;
        this.betIdempotentLogService = betIdempotentLogService;
        this.kafkaService = kafkaService;
    }

    @PostMapping(path = EndPoints.WALLET_CREATE_BET_RESULT)
    public CreateBetResultVo<Object> walletCreateBetResult(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        CreateBetResultVo<Object> createBetResultVo = new CreateBetResultVo<>();
        createBetResultVo.setRequestStartTime(System.currentTimeMillis());
        createBetResultVo.setError("ERROR");

        if (Boolean.FALSE.equals(isTestEnv)) {
            createBetResultVo.setError("SUCCESS, NOT TEST ENV");
        } else {
            try {
                String externalTransactionId = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), WalletCreateBetResultDto.class).getId();
                SettledBet settledBet = this.constructDefaultParamsForSettleBet(externalTransactionId);

                try {
                    //first check against settledBet couchbase collection
                    SettledBet cbSettledBet = settledBetService.getByVendorBetIdAndRoundIdAndVendorIdAndVendorPlayerId(settledBet.getVendorBetId(), settledBet.getRoundId(), settledBet.getVendorId(), settledBet.getVendorPlayerId());

                    //if record is found then throw BetResultIdempotentViolationException;
                    if (cbSettledBet != null) {
                        createBetResultVo.setError("SETTLEDBET DUPLICATED");
                        throw new BetResultIdempotentViolationException();
                    }

                } catch (BetNotFoundException e) {

                    //second check again betIdempotentLog couchbase collection
                    //if record is found then throw BetResultIdempotentViolationException
                    createBetResultVo.setError("BETIDEMPOTENTLOG DUPLICATED");
                    this.checkAgainstBetIdempotentLog(settledBet);

                    //will continue process as success bet continue if no record for checkAgainstBetIdempotentLog
                    String betIdempotentId = DigestUtils.md5Hex(settledBet.getVendorBetId() + "_" + settledBet.getRoundId() + "_" + settledBet.getVendorPlayerId()).toUpperCase();
                    String defaultVendorPlayerUsername = "1tuc3eB";
                    createBetResultVo.setError("SUCCESS");
                    createBetResultVo.setSettleBetId(settledBet.getId());
                    createBetResultVo.setBetIdempotentId(betIdempotentId);

                    // save into settledBet couchbase
                    settledBetService.save(settledBet, settledBet.getRawData());

                    // send settled bet to kafka
                    BetHistory betHistory = new BetHistory(settledBet);
                    kafkaService.produceBetHistory(betHistory, defaultVendorPlayerUsername, BigDecimal.ONE);

                    // insert betIdempotentLog couchbase
                    betIdempotentLogService.createForQa(settledBet);

                }

            } catch (BetResultIdempotentViolationException e) {
                httpService.logError(httpRequestLog, e);

            } catch (Exception e) {
                httpService.logError(httpRequestLog, e);

            } finally {
                createBetResultVo.setRequestEndTime(System.currentTimeMillis());
                createBetResultVo.setRequestTimeTaken(createBetResultVo.getRequestEndTime() - createBetResultVo.getRequestStartTime());
                httpService.end(httpRequestLog, createBetResultVo);

            }
        }

        return createBetResultVo;

    }

    private void checkAgainstBetIdempotentLog(SettledBet settledBet) throws BetResultIdempotentViolationException {
        RawBetIdempotentLog betIdempotentLog = null;

        if (settledBet.getVendorBetTime() != null || settledBet.getVendorSettleTime() != null) {
            Long vendorBetTime = (settledBet.getVendorBetTime() != null) ? settledBet.getVendorBetTime() : settledBet.getVendorSettleTime();
            Long timeDifference = System.currentTimeMillis() - vendorBetTime;

            //if vendorBetTime is over 2 hours, check exists against bet_idempotent_log table
            if (timeDifference > betIdempotentLogService.getTimingDifference()) {
                betIdempotentLog = betIdempotentLogService.checkExistsForQa(settledBet);

            }

        } else {
            //if vendorBetTime and vendorSettleTime is null, then check against bet_idempotent_log table
            betIdempotentLog = betIdempotentLogService.checkExistsForQa(settledBet);

        }

        if (betIdempotentLog != null) {
            throw new BetResultIdempotentViolationException(betIdempotentLog);
        }

    }

    private SettledBet constructDefaultParamsForSettleBet(String externalTransactionId) {
        SettledBet settledBet = new SettledBet();
        Long currentTimeStamp = System.currentTimeMillis();
        BigDecimal betFigure = BigDecimal.valueOf(100);
        String traceId = UUID.randomUUID().toString();

        //Player Information
        settledBet.setGameSessionToken("cd4f3cab-4ec1-495b-ab12-55dba281c51b");
        settledBet.setAgentId(12);
        settledBet.setAgentPlayerId(90394L);
        settledBet.setVendorGameId(1);
        settledBet.setVendorId(1);
        settledBet.setVendorLineId(1);
        settledBet.setVendorPlayerId(100209L);
        settledBet.setCurrencyId(2);
        settledBet.setGameCategoryId(1);

        //Bet IDs
        settledBet.setBetId(traceId);
        settledBet.setInternalTransactionId(traceId);
        settledBet.setExternalTransactionId(externalTransactionId);
        settledBet.setRoundId(externalTransactionId);
        settledBet.setVendorBetId(externalTransactionId);

        //Bet information
        settledBet.setRawData("amount=5.6&gameId=vs7monkeys&providerId=PragmaticPlay&reference=1020699969&roundDetails=spin&roundId=9488906682&timestamp=1728461324050&token=cd4f3cab-4ec1-495b-ab12-55dba281c51b&userId=1tuc3eB&hash=217d6412e48295b98681575ba2e06471");
        settledBet.setJackpotAmount(BigDecimal.ZERO);
        settledBet.setBetAmount(betFigure);
        settledBet.setWinAmount(betFigure);
        settledBet.setWinLoss(betFigure);
        settledBet.setEffectiveTurnover(betFigure);
        settledBet.setResettleNum(0);
        settledBet.setOperatorStatus(1);
        settledBet.setProcessingStatus(0);
        settledBet.setResultType(2);
        settledBet.setStatus(1);
        settledBet.setIsFreespin(0);
        settledBet.setVendorBetTime(currentTimeStamp);
        settledBet.setVendorSettleTime(currentTimeStamp);
        settledBet.setResultTime(currentTimeStamp);

        //Bet responses data
        settledBet.setCreateTime(currentTimeStamp);
        settledBet.setBalance(BigDecimal.valueOf(66666));

        //set couchbaseId
        settledBet.setId(settledBet.getVendorBetId() + '_' + settledBet.getRoundId() + '_' + settledBet.getVendorGameId() + '_' + settledBet.getVendorPlayerId());

        return settledBet;
    }
}
