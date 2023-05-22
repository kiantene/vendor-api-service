package com.nextgen.gameaggregator.vendor.ezugi.api.debit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.cq9.constant.Credentials;
import com.nextgen.gameaggregator.vendor.ezugi.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.ezugi.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.ezugi.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.RoundingMode;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class DebitAction {
    @Autowired
    private AgentApiCredentialService agentApiCredentialService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private Environment environment;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private ValidationService validationService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private WalletService walletService;

    @PostMapping(path = EndPoints.DEBIT)
    public CommonVo debit(HttpServletRequest request) throws JsonProcessingException {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        DebitVo debitVo = new DebitVo();
        try {
            String body = httpRequestLog.getRequestBody();
            DebitDto debitDto = HttpService.convertJsonToDto(body, DebitDto.class);

            //Get GameSession by player name and vendor game id
            GameSession gameSession = gameSessionService.verifyToken(debitDto.getToken());

            //Verify remaining parameters (Verify against database values)
            this.doVerification(debitDto, gameSession);

            //Get walletBalance
            BetEvent betEvent = walletService.processBet(traceId, gameSession, debitDto, body);

            // Construct Vo
            debitVo.setToken(debitDto.getToken());
            debitVo.setOperatorId(debitDto.getOperatorId());
            debitVo.setUid(gameSession.getVendorPlayerUsername());
            debitVo.setRoundId(debitDto.getVendorRoundId());
            debitVo.setTransactionId(debitDto.getTransactionId());
            debitVo.setBalance(betEvent.getLastBalance().setScale(2, RoundingMode.DOWN).doubleValue());
            debitVo.setCurrency(gameSession.getVendorCurrencyCode());
            debitVo.setErrorCode(ResponseCodes.COMPLETED_SUCCESSFULLY);
            debitVo.setErrorDescription(ResponseCodes.RESPONSE_DESCRIPTION.get(debitVo.getErrorCode()));
            debitVo.setTimestamp(System.currentTimeMillis());
        }catch (Exception e){
            httpService.logError(httpRequestLog, e);
        }finally {
            httpService.end(httpRequestLog, debitVo);
        }
        return debitVo;
    }

    private void doValidation(DebitDto debitDto, String wToken) throws InvalidRequestException, InvalidPlayerException, DateTimeParseException {
        Optional.ofNullable(wToken).orElseThrow(InvalidRequestException::new);

        // General validation
        ValidationUtils.validateRequest(debitDto);
    }

    private void doVerification(DebitDto debitDto, GameSession gameSession) throws AuthenticationException, InvalidPlayerException, CredentialNotFoundException, InvalidVendorLineException, DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException {
        // 3. Verify received game id is the same from game session
        // comparison for game session value will always be using  AuthenticationException
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), debitDto.getGameId(), AuthenticationException::new);

        //4.. validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, debitDto.getUid());
    }
}
