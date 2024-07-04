package com.nextgen.gameaggregator.vendor.saba.api.parlayconfirmbet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.BetType;
import com.nextgen.gameaggregator.exception.BetNotAllowedException;
import com.nextgen.gameaggregator.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.exception.InvalidPlayerException;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.BetIdempotentLogService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.sport.service.SportWalletService;
import com.nextgen.gameaggregator.vendor.saba.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.saba.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.saba.dto.RequestDto;
import com.nextgen.gameaggregator.vendor.saba.service.VendorService;
import com.nextgen.gameaggregator.vendor.saba.vo.GeneralVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedList;
import java.util.List;

import static com.nextgen.gameaggregator.vendor.saba.constant.EndPoints.VENDOR_CODE;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class ConfirmBetParlayAction {

    private final HttpService httpService;
    private final SportWalletService sportWalletService;
    private final WalletRequestService walletRequestService;
    private final BetIdempotentLogService betIdempotentLogService;

    @Autowired
    public ConfirmBetParlayAction(HttpService httpService,
                                  SportWalletService sportWalletService,
                                  WalletRequestService walletRequestService,
                                  BetIdempotentLogService betIdempotentLogService) {

        this.httpService = httpService;
        this.sportWalletService = sportWalletService;
        this.walletRequestService = walletRequestService;
        this.betIdempotentLogService = betIdempotentLogService;
    }

    @PostMapping(path = EndPoints.CONFIRM_BET_PARLAY)
    public GeneralVo action(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);

        // Construct Vo
        GeneralVo vo = new GeneralVo();

        try {
            RequestDto<ConfirmBetParlayDto> dto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), new TypeReference<>() {
            });

            ConfirmBetParlayDto confirmBetParlayDto = dto.getMessage();
            List<ConfirmBetParlayTxnsDto> txnList = confirmBetParlayDto.getTxns();
            boolean isMultipleBet = txnList.size() > 1;

            if (txnList.isEmpty()) {
                // catch txn array is empty and return error to vendor
                throw new InvalidRequestException("no txn found in request body");
            }

            final String vendorPlayerUsername = confirmBetParlayDto.getUserId();
            final String operationId = confirmBetParlayDto.getOperationId();
            final String roundId = this.getRoundId(txnList);

            String idempotencyKey = VENDOR_CODE + "_" + operationId;
            betIdempotentLogService.idempotentCheck(idempotencyKey);
            walletRequest.setRoundId(roundId);
            walletRequest.setNewRoundId(roundId);
            walletRequest.setVendorBetTime(System.currentTimeMillis());
            walletRequestService.updateByVendorUsername(walletRequest, vendorPlayerUsername);

            if (!isMultipleBet) { // if only 1 transaction, then don't need to use threading
                this.dataMapper(walletRequest, txnList.get(0), operationId, roundId);
                walletRequest = sportWalletService.confirmBet(walletRequest);
                vo.setBalance(walletRequest.getBalanceAfter());
                walletRequestService.end(walletRequest, httpRequestLog, vo);

            } else {
                for (ConfirmBetParlayTxnsDto txn : txnList) {
                    final WalletRequest newWalletRequest = new WalletRequest(walletRequest);
                    this.dataMapper(newWalletRequest, txn, operationId, roundId);

                    SportWalletService.THREAD_POOL.submit(() -> {
                        try {
                            sportWalletService.confirmBet(newWalletRequest);

                        } catch (Exception exception) {
                            // throw to dlq
                            log.error(exception.getMessage());
                        } finally {
                            walletRequestService.end(newWalletRequest, httpRequestLog, vo);
                        }
                    });
                }
            }

        } catch (JsonProcessingException |
                 InvalidRequestException |
                 InvalidPlayerException |
                 BetNotAllowedException exception) {
            vo.setResponseCode(ResponseCode.SYSTEM_ERROR_RETRY);
            httpService.logError(httpRequestLog, exception);
            walletRequestService.end(walletRequest, httpRequestLog, vo);

        } catch (DuplicateRequestException duplicateRequestException) {
            vo.setResponseCode(ResponseCode.DUPLICATE_TRANSACTION);
            httpService.logError(httpRequestLog, duplicateRequestException);
            walletRequestService.end(walletRequest, httpRequestLog, vo);

        } catch (Exception e) {
            vo.setResponseCode(ResponseCode.SYSTEM_ERROR_RETRY);
            httpService.logError(httpRequestLog, e);
            walletRequestService.end(walletRequest, httpRequestLog, vo);
        }

        return vo;
    }

    private void dataMapper(WalletRequest walletRequest, ConfirmBetParlayTxnsDto confirmBetParlayTxnsDto, String operationId, String roundId) {
        String refId = confirmBetParlayTxnsDto.getRefId();
        String externalTransactionId = VendorService.generateExtTxnId(operationId, refId);

        walletRequest.setExternalTransactionId(externalTransactionId);
        walletRequest.setVendorBetId(refId);
        walletRequest.setNewVendorBetId(confirmBetParlayTxnsDto.getTxId());
        walletRequest.setRoundId(roundId);
        walletRequest.setNewRoundId(roundId);
        walletRequest.setBetAmount(confirmBetParlayTxnsDto.getActualAmount());
        walletRequest.setNewBetAmount(confirmBetParlayTxnsDto.getActualAmount());
        walletRequest.setBetType(BetType.NORMAL_BET.code);
        walletRequest.setBetStatus(BetStatus.UNSETTLED);
    }

    private String getRoundId(List<ConfirmBetParlayTxnsDto> txnList) {
        boolean isMultipleBet = txnList.size() > 1;
        if (isMultipleBet) {
            return this.generateRoundId(txnList);
        } else {
            return txnList.get(0).getRefId();
        }
    }

    private String generateRoundId(List<ConfirmBetParlayTxnsDto> txnList) {
        // generate md5 vendorBetId and roundId for masterUnsettleBet using joinedRefId
        List<String> refIdList = new LinkedList<>();
        for (ConfirmBetParlayTxnsDto dto : txnList) {
            refIdList.add(dto.getRefId());
        }
        return VendorService.generateMultipleBetRoundId(refIdList);
    }
}
