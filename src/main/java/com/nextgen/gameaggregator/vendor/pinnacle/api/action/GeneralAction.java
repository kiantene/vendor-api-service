package com.nextgen.gameaggregator.vendor.pinnacle.api.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.pinnacle.api.bet.BetService;
import com.nextgen.gameaggregator.vendor.pinnacle.api.confirmbet.AcceptService;
import com.nextgen.gameaggregator.vendor.pinnacle.api.refund.RefundService;
import com.nextgen.gameaggregator.vendor.pinnacle.api.settled.SettledService;
import com.nextgen.gameaggregator.vendor.pinnacle.api.unsettle.UnsettleService;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.Endpoints;
import com.nextgen.gameaggregator.vendor.pinnacle.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.Action;
import com.nextgen.gameaggregator.vendor.pinnacle.dto.ActionsDto;
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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping(path = Endpoints.PATH)
public class GeneralAction {
    @Autowired
    private HttpService httpService;
    @Autowired
    private BetService betService;
    @Autowired
    private AcceptService acceptService;
    @Autowired
    private SettledService settledService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private RefundService refundService;
    @Autowired
    private UnsettleService unsettleService;

    @PostMapping(path = "/{agentcode}/wagering/usercode/{usercode}/request/{requestid}")
    public ResponseVo handleApiCall(@PathVariable String agentcode, @PathVariable String usercode, @PathVariable String requestid, HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);

        List<CommonVo> commonVos = Collections.emptyList();
        ResponseVo responseVo = new ResponseVo();
        ResultVo resultVo = new ResultVo();
        CommonVo commonVo = new CommonVo();

        try {
            String body = httpRequestLog.getRequestBody();
            ObjectMapper objectMapper = new ObjectMapper();
            ActionsDto dto = objectMapper.readValue(body, ActionsDto.class);

            if (dto.getActions().iterator().hasNext()) {
                Action action = dto.getActions().iterator().next();

                commonVo.setId(action.getId());
                Optional.ofNullable(action.getTransaction()).ifPresent(data -> commonVo.setTransactionId(data.getTransactionId()));
                Optional.ofNullable(action.getWagerInfo()).ifPresent(data -> commonVo.setWagerId(data.getWagerId()));
                Optional.ofNullable(action.getPlayerInfo()).ifPresent(data -> resultVo.setUserCode(data.getUserCode()));
            }

            // Get game session with username
            GameSession gameSession;
            if (Set.of("BETTED", "ACCEPTED").contains(dto.getActions().iterator().next().getName().toUpperCase())) {
                gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dto.getActions().iterator().next().getPlayerInfo().getUserCode());
            } else {
                gameSession = null;
            }

            // Process Data
            List<CompletableFuture<CommonVo>> futures = dto.getActions().stream()
                    .map(action -> CompletableFuture.supplyAsync(() -> actionsSwitching(action, gameSession, httpRequestLog)))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // Combine the results
            commonVos = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();


        } catch (Exception exception) {
            httpService.logError(httpRequestLog, exception);
            commonVo.setResponseCode(ResponseCode.UNKNOWN_ERROR.code);

        } finally {
            resultVo.setAvailableBalance((commonVos.isEmpty() || commonVos.iterator().next().getBalance() == null) ? BigDecimal.ZERO : commonVos.iterator().next().getBalance());
            resultVo.setActions(commonVos.isEmpty() ? Collections.singletonList(commonVo) : commonVos);
            if (commonVos.stream().anyMatch(CommonVo::getSetResponseVoErrorCode))
                responseVo.setErrorCode(ResponseCode.UNKNOWN_ERROR.code);
            responseVo.setResult(resultVo);
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private CommonVo actionsSwitching(Action action, GameSession gameSession, HttpRequestLog httpRequestLog) {

        return switch (action.getName().toUpperCase()) {
            case "BETTED" -> betService.bet(action, gameSession, httpRequestLog);
            case "ACCEPTED" -> acceptService.accept(action, gameSession, httpRequestLog);
            case "SETTLED" -> settledService.settled(action, httpRequestLog);
            case "REJECTED", "ROLLBACKED", "CANCELLED" -> refundService.refund(action, httpRequestLog);
            case "UNSETTLED" -> unsettleService.unsettle(action, httpRequestLog);
            default -> throw new IllegalStateException("Unexpected value: " + action.getName());
        };
    }
}

