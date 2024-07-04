package com.nextgen.gameaggregator.vendor.pinnacle.api.action;

import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.*;
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

import java.util.*;

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

    @Autowired
    public GeneralAction(HttpService httpService,
                         BetService betService,
                         AcceptService acceptService,
                         SettledService settledService,
                         RefundService refundService,
                         UnsettleService unsettleService,
                         WalletRequestService walletRequestService) {

        this.httpService = httpService;
        this.betService = betService;
        this.acceptService = acceptService;
        this.settledService = settledService;
        this.refundService = refundService;
        this.unsettleService = unsettleService;
        this.walletRequestService = walletRequestService;
    }

    @PostMapping(path = "/{agentCode}/wagering/usercode/{userCode}/request/{requestId}")
    public ResponseVo handleApiCall(@PathVariable String agentCode, @PathVariable String userCode, @PathVariable String requestId, HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);

        List<CommonVo> commonVoList = new LinkedList<>();
        ResponseVo responseVo = new ResponseVo();
        ResultVo resultVo = new ResultVo();
        CommonVo commonVo = new CommonVo();
        Action action = null;

        try {
            ActionsDto actionsDto = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), ActionsDto.class);

            this.doValidation(actionsDto);

            action = actionsDto.getActions().get(0);
            ActionsTransactionDto transactionDto = action.getTransaction();
            ActionsWagerInfoDto wagerInfoDto = action.getWagerInfo();

            Long transactionId = Optional.ofNullable(transactionDto).map(ActionsTransactionDto::getTransactionId).orElse(null);
            Long wagerId = wagerInfoDto.getWagerId();
            commonVo = new CommonVo(action.getId(), transactionId, wagerId);
            resultVo.setUserCode(userCode);

            this.dataMapper(walletRequest, userCode);

            // Process Data
            for (Action data : actionsDto.getActions()) {
                commonVo = this.actionsSwitching(data, httpRequestLog, walletRequest);
                commonVoList.add(commonVo);
            }

        } catch (InsufficientBalanceException e) {
            httpService.logError(httpRequestLog, e);
            walletRequest.setErrorMessage(e.getMessage());
            commonVo.setResponseCode(ResponseCode.INSUFFICIENT_FUND.code);
            commonVoList.add(commonVo);

        } catch (Exception exception) {
            httpService.logError(httpRequestLog, exception);
            walletRequest.setErrorMessage(exception.getMessage());
            commonVo.setResponseCode(ResponseCode.UNKNOWN_ERROR.code);
            commonVoList.add(commonVo);

        } finally {
            resultVo.setAvailableBalance(commonVoList.get(0).getBalance());
            resultVo.setActions(commonVoList);
            responseVo.setResult(resultVo);
            if (Set.of("BETTED", "ACCEPTED").contains(Objects.requireNonNull(action).getName().toUpperCase())) {
                walletRequestService.end(walletRequest, httpRequestLog, responseVo);
            } else {
                httpService.end(httpRequestLog, responseVo);
            }
        }

        return responseVo;
    }

    private CommonVo actionsSwitching(Action action, HttpRequestLog httpRequestLog, WalletRequest walletRequest) throws
            AuthenticationException, InvalidRequestException, BetResultIdempotentViolationException,
            InsufficientBalanceException, TransactionStillProcessingException, InvalidOperatorResponseException,
            BetNotFoundException, BetNotAllowedException, InvalidPlayerException {

        return switch (action.getName().toUpperCase()) {
            case "BETTED" -> betService.bet(walletRequest, action);
            case "ACCEPTED" -> acceptService.accept(walletRequest, action);
            case "SETTLED" -> settledService.settled(action, httpRequestLog);
            case "REJECTED", "ROLLBACKED", "CANCELLED" -> refundService.refund(action, httpRequestLog);
            case "UNSETTLED" -> unsettleService.unsettle(action, httpRequestLog);
            default -> throw new IllegalStateException("Unexpected value: " + action.getName());
        };
    }

    private void dataMapper(WalletRequest walletRequest, String username) {
        walletRequest.setVendorPlayerUsername(username);
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

