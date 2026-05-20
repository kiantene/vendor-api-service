package com.nextgen.gameaggregator.vendor.pinnacle.api.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.data.kafka.betdetails.BetDetailEmitRequest;
import com.nextgen.gameaggregator.data.kafka.betdetails.EventKind;
import com.nextgen.gameaggregator.data.kafka.betdetails.RawSportsBetDetailsProducer;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.BetIdempotentLogService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.pinnacle.api.bet.BetService;
import com.nextgen.gameaggregator.vendor.pinnacle.api.confirmbet.AcceptService;
import com.nextgen.gameaggregator.vendor.pinnacle.api.refund.RefundService;
import com.nextgen.gameaggregator.vendor.pinnacle.api.settled.SettledService;
import com.nextgen.gameaggregator.vendor.pinnacle.api.unsettle.UnsettleService;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.Action;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.ActionsDto;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.ActionsTransactionDto;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.ActionsWagerInfoDto;
import com.nextgen.gameaggregator.vendor.pinnacle.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.pinnacle.vo.ResponseVo;
import com.nextgen.gameaggregator.vendor.pinnacle.vo.ResultVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping(path = Endpoints.PATH)
public class GeneralAction {
    private final HttpService httpService;
    private final BetService betService;
    private final AcceptService acceptService;
    private final SettledService settledService;
    private final RefundService refundService;
    private final UnsettleService unsettleService;
    private final WalletRequestService walletRequestService;
    private final BetIdempotentLogService betIdempotentLogService;
    private final RawSportsBetDetailsProducer rawSportsBetDetailsProducer;
    private final ObjectMapper objectMapper;

    private static final String VENDOR = "pinnacle";
    private static final String EVENT_FAMILY = "wagering";

    @Autowired
    public GeneralAction(HttpService httpService,
                         BetService betService,
                         AcceptService acceptService,
                         SettledService settledService,
                         RefundService refundService,
                         UnsettleService unsettleService,
                         WalletRequestService walletRequestService,
                         BetIdempotentLogService betIdempotentLogService,
                         RawSportsBetDetailsProducer rawSportsBetDetailsProducer,
                         ObjectMapper objectMapper) {

        this.httpService = httpService;
        this.betService = betService;
        this.acceptService = acceptService;
        this.settledService = settledService;
        this.refundService = refundService;
        this.unsettleService = unsettleService;
        this.walletRequestService = walletRequestService;
        this.betIdempotentLogService = betIdempotentLogService;
        this.rawSportsBetDetailsProducer = rawSportsBetDetailsProducer;
        this.objectMapper = objectMapper;
    }

    @PostMapping(path = "/{agentCode}/wagering/usercode/{userCode}/request/{requestId}")
    public ResponseVo handleApiCall(@PathVariable String agentCode,
                                    @PathVariable(value = "userCode") String vendorPlayerUsername,
                                    @PathVariable String requestId,
                                    HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);

        List<CommonVo> commonVoList = new LinkedList<>();
        ResponseVo responseVo = new ResponseVo();
        ResultVo resultVo = new ResultVo();
        CommonVo commonVo = new CommonVo();
        boolean isDuplicatedRequest = false;
        String idempotentKey = "";

        try {
            // Dto validation
            ActionsDto actionsDto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), ActionsDto.class);
            this.doValidation(actionsDto);

            // Extract Dto
            Action action = actionsDto.getActions().get(0);
            ActionsTransactionDto transactionDto = action.getTransaction();
            ActionsWagerInfoDto wagerInfoDto = action.getWagerInfo();

            // Idempotent checking
            String externalTransactionId = action.getId().toString();
            idempotentKey = vendorPlayerUsername + "_" + externalTransactionId;
            //block Idempotent request
            betIdempotentLogService.idempotentCheck(idempotentKey);

            // Setup Response Vo
            Long transactionId = Optional.ofNullable(transactionDto).map(ActionsTransactionDto::getTransactionId).orElse(null);
            Long wagerId = wagerInfoDto.getWagerId();
            commonVo = new CommonVo(action.getId(), transactionId, wagerId);
            resultVo.setUserCode(vendorPlayerUsername);

            this.dataMapper(walletRequest, vendorPlayerUsername, externalTransactionId);

            // Process Data
            for (Action data : actionsDto.getActions()) {
                commonVo = this.actionsSwitching(data, walletRequest, commonVo);
                this.emitRawBetDetail(data, walletRequest);
                commonVoList.add(commonVo);
            }

        } catch (InsufficientBalanceException insufficientBalanceException) {
            this.logException(walletRequest, insufficientBalanceException);
            commonVo.setResponseCode(ResponseCode.INSUFFICIENT_FUND.code);
            commonVoList.add(commonVo);

        } catch (DuplicateRequestException ex) {
            this.logException(walletRequest, ex);
            commonVo.setResponseCode(ResponseCode.UNKNOWN_ERROR.code);
            commonVoList.add(commonVo);
            isDuplicatedRequest = true;

        } catch (Exception exception) {
            this.logException(walletRequest, exception);
            commonVo.setResponseCode(ResponseCode.UNKNOWN_ERROR.code);
            commonVoList.add(commonVo);

        } finally {
            if (isDuplicatedRequest == false) {
                betIdempotentLogService.delete(idempotentKey);
            }
            resultVo.setAvailableBalance(commonVoList.get(0).getBalance());
            resultVo.setActions(commonVoList);
            responseVo.setResult(resultVo);
            walletRequestService.end(walletRequest, httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private CommonVo actionsSwitching(Action action, WalletRequest walletRequest, CommonVo commonVo) throws
            AuthenticationException, InvalidRequestException, BetResultIdempotentViolationException,
            InsufficientBalanceException, TransactionStillProcessingException, InvalidOperatorResponseException,
            BetNotFoundException, BetNotAllowedException, InvalidPlayerException, BetFailedException {

        return switch (action.getName().toUpperCase()) {
            case "BETTED" -> betService.bet(walletRequest, action, commonVo);
            case "ACCEPTED" -> acceptService.accept(walletRequest, action, commonVo);
            case "SETTLED" -> settledService.settled(walletRequest, action, commonVo);
            case "REJECTED", "ROLLBACKED", "CANCELLED" -> refundService.refund(walletRequest, action, commonVo);
            case "UNSETTLED" -> unsettleService.unsettle(walletRequest, action, commonVo);
            default -> throw new IllegalStateException("Unexpected value: " + action.getName());
        };
    }

    private void dataMapper(WalletRequest walletRequest, String vendorPlayerUsername, String externalTransactionId) {
        walletRequest.setVendorPlayerUsername(vendorPlayerUsername);
        walletRequest.setExternalTransactionId(externalTransactionId);
    }

    private void emitRawBetDetail(Action action, WalletRequest walletRequest) {
        try {
            if (action == null || action.getWagerInfo() == null || action.getWagerInfo().getWagerId() == null) {
                log.warn("Skipping Pinnacle wagering emit: missing wagerId actionName={}", action == null ? null : action.getName());
                return;
            }
            EventKind eventKind = mapEventKind(action.getName());
            if (eventKind == null) {
                log.warn("Skipping Pinnacle wagering emit: unmapped actionName={}", action.getName());
                return;
            }
            if (walletRequest == null) {
                log.warn("Skipping Pinnacle wagering emit: walletRequest is null actionName={}", action.getName());
                return;
            }
            // Each service (BetService/AcceptService/SettledService/etc.) sets walletRequest.vendorBetId
            // and roundId to the normalized/persisted IDs (e.g. AcceptService remaps multi-leg updates to
            // wagerMasterId_wagerNum with roundId=wagerMasterId). Honor those so downstream emits line
            // up with the stored bets rather than the raw wagerId on the action.
            String vendorBetId = walletRequest.getVendorBetId();
            String roundId = walletRequest.getRoundId();
            if (vendorBetId == null || roundId == null) {
                log.warn("Skipping Pinnacle wagering emit: walletRequest missing ids actionName={} vendorBetId={} roundId={}",
                        action.getName(), vendorBetId, roundId);
                return;
            }
            // Pinnacle may re-issue SETTLED for the same wager (resettle) — WagerInfo.resettlementTime
            // is populated on the resettle callback. Without a discriminator the resettle and the
            // original settle produce the same `vendor:vendorBetId:RESULT_UPDATE` key, so Stage-2
            // would collapse them. action.Id is unique per action, so we use it as the version
            // suffix on resettles only. The same rule must be mirrored in C.1.
            Long resettleVersion = action.getWagerInfo().getResettlementTime() != null ? action.getId() : null;
            String body = objectMapper.writeValueAsString(action);
            rawSportsBetDetailsProducer.emit(BetDetailEmitRequest.builder()
                    .vendor(VENDOR)
                    .eventFamily(EVENT_FAMILY)
                    .eventKind(eventKind)
                    .vendorBetId(vendorBetId)
                    .gaBetId(walletRequest.getBetId())
                    .roundId(roundId)
                    .vendorPlayerUsername(walletRequest.getVendorPlayerUsername())
                    .agentId(walletRequest.getAgentId())
                    .requestBody(body)
                    .resettleVersion(resettleVersion)
                    .build());
        } catch (Exception e) {
            // emit-only — never block the wallet path
            log.warn("Pinnacle wagering emit failed actionName={}: {}", action == null ? null : action.getName(), e.getMessage());
        }
    }

    private static EventKind mapEventKind(String actionName) {
        if (actionName == null) return null;
        return switch (actionName.toUpperCase()) {
            case "BETTED" -> EventKind.PLACE_BET;
            case "ACCEPTED", "REJECTED", "ROLLBACKED", "CANCELLED", "UNSETTLED" -> EventKind.UPDATE_BET;
            case "SETTLED" -> EventKind.RESULT_UPDATE;
            default -> null;
        };
    }

    private void doValidation(ActionsDto actionsDto) throws InvalidRequestException {
        // General validation
        try {
            ValidationUtils.validateRequest(actionsDto);

        } catch (InvalidRequestException e) {
            throw new InvalidRequestException(e.getValidation().values().stream().findFirst().orElse("Invalid Request Body"));
        }
    }

    public static Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }

    public void logException(WalletRequest walletRequest, Exception exception) {
        if (exception.getCause() != null) {
            Throwable rootCause = getRootCause(exception.getCause());
            walletRequest.setErrorMessage(rootCause.getClass().getSimpleName() + " - " + rootCause.getMessage());
        } else if (exception.getMessage() != null) {
            walletRequest.setErrorMessage(exception.getClass().getSimpleName() + " - " + exception.getMessage());
        } else {
            walletRequest.setErrorMessage(exception.getClass().getSimpleName());
        }

    }

}

