package com.nextgen.gameaggregator.vendor.pinnacle.api.action;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.nextgen.gameaggregator.entity.ga.VendorGame;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.repository.ga.writer.PinnacleVendorUsernameRepository;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.WalletService;
import com.nextgen.gameaggregator.vendor.pinnacle.api.accept.AcceptService;
import com.nextgen.gameaggregator.vendor.pinnacle.api.bet.BetService;
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
    private WalletService walletService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private RefundService refundService;
    @Autowired
    private UnsettleService unsettleService;
    @Autowired
    private PinnacleVendorUsernameRepository pinnacleVendorUsernameRepository;

    @PostMapping(path = "/{agentcode}/wagering/usercode/{usercode}/request/{requestid}")
    public ResponseVo handleApiCall(@PathVariable String agentcode, @PathVariable String usercode, @PathVariable String requestid, HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        ResponseVo responseVo = new ResponseVo();
        Integer errorCode = ResponseCode.UNKNOWN_ERROR.code;

        try {
            String body = httpRequestLog.getRequestBody();
            ObjectMapper objectMapper = new ObjectMapper();
            ActionsDto dto = objectMapper.readValue(body, ActionsDto.class);

            // Get GA username from couchbase
            String userCode = dto.getActions().get(0).getPlayerInfo().getUserCode();
            Optional<VendorGame.PinnacleVendorPlayer> player = pinnacleVendorUsernameRepository.findByVendorPlayerUsername(userCode);

            // Get game session with username
            String username = player.get().getUsername();
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(username);

            // Create a set to store action name
            Set<String> uniqueActionNames = new HashSet<>();

            List<CompletableFuture<List<CommonVo>>> futures = dto.getActions().stream()
                    .filter(action -> uniqueActionNames.add(action.getName())) // Only consider unique action names
                    .map(action -> CompletableFuture.supplyAsync(() -> actionsSwitching(action.getName(), dto, gameSession, httpRequestLog)))
                    .collect(Collectors.toList());

            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            allOf.join(); // Wait for all services to complete

            // Combine the results
            List<CommonVo> commonVos = futures.stream()
                    .map(CompletableFuture::join)
                    .flatMap(List::stream) // Flatten the list of lists into a single list
                    .collect(Collectors.toList());

            responseVo = mergeResponses(commonVos, dto.getActions().get(0), gameSession, httpRequestLog);

        } catch (Exception exception) {
            httpService.logError(httpRequestLog, exception);
            responseVo.setErrorCode(errorCode);

        } finally {
            httpService.end(httpRequestLog, responseVo);
        }

        return responseVo;
    }

    private List<CommonVo> actionsSwitching(String actionName, ActionsDto dto, GameSession gameSession, HttpRequestLog httpRequestLog) {
        List<CommonVo> commonVos = new ArrayList<>();
    
        try {
            switch (actionName) {
                case "BETTED" -> commonVos.addAll(betService.bet(dto, gameSession, httpRequestLog));
                case "ACCEPTED" -> commonVos.addAll(acceptService.accept(dto, gameSession, httpRequestLog));
                case "SETTLED" -> commonVos.addAll(settledService.settled(dto, gameSession, httpRequestLog));
                case "REJECTED", "ROLLBACKED", "CANCELLED" -> commonVos.addAll(refundService.refund(dto, gameSession, httpRequestLog));
                case "UNSETTLED" -> commonVos.addAll(unsettleService.unsettle(dto, gameSession, httpRequestLog));
                default -> {
                    CommonVo commonVo = new CommonVo();
                    commonVo.setResponseCode(ResponseCode.UNKNOWN_ERROR.code);
                    commonVos.add(commonVo);
                }
            }
        } catch (Exception e) {
            CommonVo commonVo = new CommonVo();
            commonVo.setResponseCode(ResponseCode.UNKNOWN_ERROR.code);
            commonVos.add(commonVo);
        }
    
        return commonVos;
    }    

    private ResponseVo mergeResponses(List<CommonVo> commonVos, Action action, GameSession gameSession, HttpRequestLog httpRequestLog) {
        ResponseVo responseVo = new ResponseVo();
        ResultVo result = new ResultVo();
        String traceId = httpRequestLog.getId();

        try {
            // Check if any CommonVo has error
            boolean hasUnknownError = commonVos.stream().anyMatch(commonVo -> commonVo.getResponseCode() != 0);
    
            // Get latest balance after all actions are done
            BigDecimal balance = walletService.getBalance(traceId, gameSession, httpRequestLog);
            result.setUserCode(action.getPlayerInfo().getUserCode());
            result.setAvailableBalance(balance);
            result.setActions(commonVos);
            
            // Set errorCode based on the condition
            responseVo.setErrorCode(hasUnknownError ? ResponseCode.UNKNOWN_ERROR.code : ResponseCode.SUCCESS.code);
            
            responseVo.setResult(result);
    
        } catch (Exception e) {
            log.error("Exception: " + e.getMessage());
        }

        return responseVo;
    }
}

