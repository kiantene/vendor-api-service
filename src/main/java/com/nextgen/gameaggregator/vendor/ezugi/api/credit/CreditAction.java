package com.nextgen.gameaggregator.vendor.ezugi.api.credit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.cq9.api.endround.EndRoundDataDto;
import com.nextgen.gameaggregator.vendor.cq9.api.endround.EndRoundDto;
import com.nextgen.gameaggregator.vendor.cq9.constant.Credentials;
import com.nextgen.gameaggregator.vendor.ezugi.api.authentication.AuthenticationDto;
import com.nextgen.gameaggregator.vendor.ezugi.api.authentication.AuthenticationVo;
import com.nextgen.gameaggregator.vendor.ezugi.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.ezugi.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.ezugi.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.ezugi.vo.CommonVo;
import com.nextgen.gameaggregator.vendor.jdb.api.endround.BetNSettleDto;
import com.nextgen.gameaggregator.vendor.jdb.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class CreditAction extends CommonDto {
    @Autowired
    private HttpService httpService;
    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private VendorService vendorService;

    @PostMapping(path = EndPoints.CREDIT)
    public CommonVo credit(HttpServletRequest request) throws JsonProcessingException {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        CreditVo creditVo = new CreditVo();
        try {
            String body = httpRequestLog.getRequestBody();
            CreditDto creditDto = HttpService.convertJsonToDto(body, CreditDto.class);

            //Get and set bet data Object from body
            //GameDataStringDto gameDataStringDto = HttpService.convertJsonToDto(creditDto.getGameDataString(), GameDataStringDto.class);
            //creditDto.setGameDataStringDto(gameDataStringDto);

            //Get GameSession by player name and vendor game id
            GameSession gameSession = gameSessionService.verifyToken(creditDto.getToken());

            //Verify remaining parameters (Verify against database values)
            this.doVerification(creditDto, gameSession);

            //Process result settle data
            ResultType resultType = getResultType(creditDto);
            BigDecimal balance = walletService.processBetResult(traceId, gameSession, creditDto, resultType, vendorService, body);

            // Construct Vo
            creditVo.setToken(creditDto.getToken());
            creditVo.setOperatorId(creditDto.getOperatorId());
            creditVo.setUid(gameSession.getVendorPlayerUsername());
            creditVo.setRoundId(creditDto.getRoundId());
            creditVo.setTransactionId(creditDto.getTransactionId());
            creditVo.setBalance(balance.setScale(2, RoundingMode.DOWN).doubleValue());
            creditVo.setCurrency(gameSession.getVendorCurrencyCode());
            creditVo.setErrorCode(ResponseCodes.COMPLETED_SUCCESSFULLY);
            creditVo.setErrorDescription(ResponseCodes.RESPONSE_DESCRIPTION.get(creditVo.getErrorCode()));
            creditVo.setTimestamp(System.currentTimeMillis());
        }catch (Exception e){
            httpService.logError(httpRequestLog, e);
        }finally {
            httpService.end(httpRequestLog, creditVo);
        }
        return creditVo;
    }


    private void doValidation(CreditDto creditDto) throws InvalidRequestException, InvalidPlayerException, DateTimeParseException {
        // General validation
        ValidationUtils.validateRequest(creditDto);
    }

    private void doVerification(CreditDto dto, GameSession gameSession) throws InvalidPlayerException, AuthenticationException, CredentialNotFoundException, InvalidVendorLineException {
        // 1. Verify received username is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), dto.getUid(), InvalidPlayerException::new);

        // 2. Verify received game id is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), dto.getGameId(), AuthenticationException::new);
    }
    private ResultType getResultType(CreditDto dto) {

        ResultType resultType = ResultType.BET_LOSE;
        BigDecimal zero = BigDecimal.ZERO;

        if (dto.getWinAmount().compareTo(zero) > 0) { // Win Amount > 0 ~ BET_WIN
            resultType = ResultType.BET_WIN;
        }
        if (dto.getWinAmount().compareTo(zero) == 0 && dto.getBetAmount().compareTo(zero) == 0) { // Win Amount == 0 and Bet Amount == 0 ~ BET_WIN
            resultType = ResultType.BET_WIN;
        }

        return resultType;
    }

}
