package com.nextgen.gameaggregator.vendor.pinnacle.api.action;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
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

    @Autowired
    public GeneralAction(HttpService httpService,
                         BetService betService,
                         AcceptService acceptService,
                         SettledService settledService,
                         RefundService refundService,
                         UnsettleService unsettleService,
                         WalletRequestService walletRequestService,
                         BetIdempotentLogService betIdempotentLogService) {

        this.httpService = httpService;
        this.betService = betService;
        this.acceptService = acceptService;
        this.settledService = settledService;
        this.refundService = refundService;
        this.unsettleService = unsettleService;
        this.walletRequestService = walletRequestService;
        this.betIdempotentLogService = betIdempotentLogService;
    }

    @PostMapping(path = "/{agentCode}/wagering/usercode/{userCode}/request/{requestId}")
    public ResponseVo handleApiCall(@PathVariable String agentCode, @PathVariable(value = "userCode") String vendorPlayerUsername, @PathVariable String requestId, HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);

        List<CommonVo> commonVoList = new LinkedList<>();
        ResponseVo responseVo = new ResponseVo();
        ResultVo resultVo = new ResultVo();
        CommonVo commonVo = new CommonVo();

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
            String idempotentKey = vendorPlayerUsername + "_" + externalTransactionId;
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
                commonVoList.add(commonVo);
            }

        } catch (InsufficientBalanceException insufficientBalanceException) {
            walletRequest.setErrorMessage(insufficientBalanceException.toString());
            commonVo.setResponseCode(ResponseCode.INSUFFICIENT_FUND.code);
            commonVoList.add(commonVo);

        } catch (Exception exception) {
            walletRequest.setErrorMessage(exception.toString());
            commonVo.setResponseCode(ResponseCode.UNKNOWN_ERROR.code);
            commonVoList.add(commonVo);

        } finally {
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

    private void doValidation(ActionsDto actionsDto) throws InvalidRequestException {
        // General validation
        try {
            ValidationUtils.validateRequest(actionsDto);

        } catch (InvalidRequestException e) {
            throw new InvalidRequestException(e.getValidation().values().stream().findFirst().orElse("Invalid Request Body"));
        }
    }
}

