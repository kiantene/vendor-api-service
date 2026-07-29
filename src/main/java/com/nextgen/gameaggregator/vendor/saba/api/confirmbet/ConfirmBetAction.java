package com.nextgen.gameaggregator.vendor.saba.api.confirmbet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.data.kafka.betdetails.BetDetailEmitRequest;
import com.nextgen.gameaggregator.data.kafka.betdetails.EventKind;
import com.nextgen.gameaggregator.data.kafka.betdetails.RawSportsBetDetailsProducer;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.BetType;
import com.nextgen.gameaggregator.exception.BetResultIdempotentViolationException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.sport.service.SportWalletService;
import com.nextgen.gameaggregator.vendor.saba.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.saba.constant.OddsType;
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

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class ConfirmBetAction {

    private final HttpService httpService;
    private final SportWalletService sportWalletService;
    private final WalletRequestService walletRequestService;
    private final RawSportsBetDetailsProducer rawSportsBetDetailsProducer;

    private static final String VENDOR = "saba";
    private static final String EVENT_FAMILY = "confirmbet";

    @Autowired
    public ConfirmBetAction(HttpService httpService,
                            SportWalletService sportWalletService,
                            WalletRequestService walletRequestService,
                            RawSportsBetDetailsProducer rawSportsBetDetailsProducer) {

        this.httpService = httpService;
        this.sportWalletService = sportWalletService;
        this.walletRequestService = walletRequestService;
        this.rawSportsBetDetailsProducer = rawSportsBetDetailsProducer;
    }

    @PostMapping(path = EndPoints.CONFIRM_BET)
    public GeneralVo action(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);

        // Construct Vo
        GeneralVo vo = new GeneralVo();

        try {
            // Convert original request body into dto
            RequestDto<ConfirmBetDto> dto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), new TypeReference<>() {
            });

            this.dataMapper(walletRequest, dto.getMessage());

            // 4. Process unsettle data
            sportWalletService.confirmBet(walletRequest);

            this.emitRawBetDetail(walletRequest, dto.getMessage(), httpRequestLog.getRequestBody());

            vo.setResponseCode(ResponseCode.SUCCESS);
            vo.setBalance(walletRequest.getBalanceAfter());

        } catch (BetResultIdempotentViolationException e) {
            vo.setResponseCode(ResponseCode.SUCCESS);
            vo.setBalance(walletRequest.getBalanceAfter());
            httpService.logError(httpRequestLog, e);

        } catch (Exception e) {
            vo.setResponseCode(ResponseCode.SYSTEM_ERROR_RETRY);
            vo.setMsg(ResponseCode.SYSTEM_ERROR_RETRY.message);
            httpService.logError(httpRequestLog, e);
            walletRequest.setErrorMessage(e.getMessage());

        } finally {
            //after process, vendor bet id will set as newest for logging purpose.
            walletRequest.setVendorBetId(walletRequest.getNewVendorBetId());
            walletRequestService.end(walletRequest, httpRequestLog, vo);

        }

        return vo;
    }

    private void emitRawBetDetail(WalletRequest walletRequest, ConfirmBetDto dto, String requestBody) {
        try {
            if (dto == null || dto.getTxns() == null || dto.getTxns().isEmpty()) {
                log.warn("Skipping SABA confirmbet emit: missing txns traceId={}", walletRequest == null ? null : walletRequest.getTraceId());
                return;
            }
            String txId = dto.getTxns().get(0).getTxId() == null ? null : dto.getTxns().get(0).getTxId().toString();
            String refId = dto.getTxns().get(0).getRefId();
            if (txId == null || refId == null) {
                log.warn("Skipping SABA confirmbet emit: missing required fields txId={} refId={}", txId, refId);
                return;
            }
            rawSportsBetDetailsProducer.emit(BetDetailEmitRequest.builder()
                    .vendor(VENDOR)
                    .eventFamily(EVENT_FAMILY)
                    .eventKind(EventKind.UPDATE_BET)
                    .vendorBetId(txId)
                    .gaBetId(walletRequest.getBetId())
                    .roundId(refId)
                    .vendorPlayerUsername(walletRequest.getVendorPlayerUsername())
                    .agentId(walletRequest.getAgentId())
                    .requestBody(requestBody)
                    .build());
        } catch (Exception e) {
            // emit-only — never block the wallet path
            log.warn("SABA confirmbet emit failed: {}", e.getMessage());
        }
    }

    private void dataMapper(WalletRequest walletRequest, ConfirmBetDto dto) {
        String refId = dto.getTxns().get(0).getRefId();
        String txId = dto.getTxns().get(0).getTxId().toString();
        String externalTransactionId = VendorService.generateExtTxnId(dto.getOperationId(), refId);

        walletRequest.setExternalTransactionId(externalTransactionId);
        walletRequest.setVendorPlayerUsername(dto.getUserId());
        walletRequest.setVendorBetId(refId);
        walletRequest.setNewVendorBetId(txId);
        walletRequest.setRoundId(refId);
        walletRequest.setVendorBetTime(System.currentTimeMillis());
        walletRequest.setBetType(BetType.NORMAL_BET.code);
        walletRequest.setBetStatus(BetStatus.UNSETTLED);
        walletRequest.setNewBetAmount(dto.getTxns().get(0).getActualAmount());
        walletRequest.setOddsType(OddsType.convertToSportOddsCode(dto.getTxns().get(0).getOddsType()));
        walletRequest.setOdds(dto.getTxns().get(0).getOdds());
    }
}
