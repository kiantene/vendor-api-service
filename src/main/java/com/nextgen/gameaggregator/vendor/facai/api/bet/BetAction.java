package com.nextgen.gameaggregator.vendor.facai.api.bet;

import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.entity.VendorPlayer;
import com.nextgen.gameaggregator.enums.WinType;
import com.nextgen.gameaggregator.eventing.core.EventDispatcherSystem;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.eventing.events.BetResultEvent;
import com.nextgen.gameaggregator.eventing.events.EndRoundEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.facai.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.facai.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.facai.dto.CommonDto;
import com.nextgen.gameaggregator.vendor.facai.service.VendorService;
import com.nextgen.gameaggregator.vendor.facai.vo.CommonVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class BetAction {

    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private VendorLineService vendorLineService;
    @Autowired
    private AgentPlayerService agentPlayerService;
    @Autowired
    private VendorGameService vendorGameService;
    @Autowired
    private VendorPlayerService vendorPlayerService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VendorService vendorService;

    @PostMapping(path = EndPoints.SLOT_BET)
    public CommonVo bet(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getTraceId();

        // Construct VO
        CommonVo commonVo = new CommonVo();
        //betVo.setResult(0);
        //betVo.setMainPoints(1000.00);

        try {
            //Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            //Convert original request body into commonDto
            CommonDto commonDto = HttpService.convertQueryStringToDtoUrlDecode(body, CommonDto.class);

            //TODO pending PG update core function to get appKey
            //Decrypt raw respond
            String jsonParam = vendorService.aesDecrypt(commonDto.getParams(), "Q7RaR8CUbwZ0roD2");

            //map decrypted data(string json) into balanceDto
            VendorBetDto vendorBetDto = HttpService.convertJsonToDto(jsonParam, VendorBetDto.class);

            //get gameSession by player name
            VendorPlayer vendorPlayer = vendorPlayerService.getVendorPlayerByUsername(vendorBetDto.memberAccount);
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(vendorPlayer.getUsername());

            //Verify remaining parameters (Verify against database values)
            this.doVerification(vendorBetDto, gameSession);

            //Construct BetDto
            BetDto betDto = new BetDto();
            betDto.setExternalTransactionId(Long.toString(vendorBetDto.getBankID()));
            betDto.setRoundId(vendorBetDto.getRecordID());
            betDto.setAmount(vendorBetDto.getBet());
            betDto.setGameCode(Integer.toString(vendorBetDto.getGameID()));
            betDto.setEventTime(vendorBetDto.getCreateDate());
            //Send bet request to Operator
            //check if player has enough balance
            //used database constraint to check duplicate bet request based on external_transaction_id, round_id, vendor_line_id
            log.info(betDto.toString());
            BetEvent betEvent = walletService.processBet(traceId, gameSession, betDto, body);

            //Construct WinDataDto
            WinDataDto winDataDto = new WinDataDto();
            winDataDto.setExternalTransactionId(Long.toString(vendorBetDto.getBankID()));
            winDataDto.setRoundid(vendorBetDto.getRecordID());
            winDataDto.setAmount(vendorBetDto.getWin());
            winDataDto.setGamecode(Integer.toString(vendorBetDto.getGameID()));
            winDataDto.setEventTime(vendorBetDto.getGameDate());
            winDataDto.setWinType(this.getWinType(vendorBetDto));
            winDataDto.setEffectiveTurnover(vendorBetDto.getBet());
            BetResultEvent betResultEvent = walletService.processWin(traceId, gameSession, winDataDto, body);

            //Emit event for additional asynchronous processing
            EventDispatcherSystem.emitAsync(new EndRoundEvent(betResultEvent.getBetHistory()));

            //set VO data
            //convert bigDecimal balance into double
            commonVo.setResult(ResponseCodes.SUCCESS);
            commonVo.setMainPoints(betResultEvent.getLastBalance().setScale(2,RoundingMode.DOWN).doubleValue());

        }catch (Exception exception) {
            commonVo.setResult(ResponseCodes.UNEXPECTED_ERROR);
            commonVo.setErrorText(ResponseCodes.UNEXPECTED_ERROR_MSG);
        }finally {
            httpService.end(httpRequestLog, commonVo);
        }

        return commonVo;
    }

    private void doVerification(VendorBetDto vendorBetDto, GameSession gameSession) throws AuthenticationException, InvalidPlayerException, CredentialNotFoundException, InvalidVendorLineException, DisabledVendorLineException, DisabledAgentPlayerException, DisabledGameException {

        //Verify received username is the same from game session
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), vendorBetDto.getMemberAccount(), InvalidPlayerException::new);

        //Verify received game id is the same from game session
        // comparison for game session value will always be using  AuthenticationException
        ValidationUtils.isEquals(gameSession.getVendorGameCode(), Integer.toString(vendorBetDto.getGameID()), AuthenticationException::new);

        //Verify vendor line is active
        vendorLineService.verifyVendorLineStatus(gameSession.getVendorLineId());

        //Verify agent player is active
        agentPlayerService.verifyAgentPlayerStatus(gameSession.getAgentPlayerId());

        //Verify vendor game is active
        vendorGameService.verifyGameStatus(gameSession.getVendorGameId());
    }

    private WinType getWinType(VendorBetDto vendorBetDto) {
        WinType winType;

        winType = (vendorBetDto.getWin().compareTo(BigDecimal.ZERO) > 0) ? WinType.WIN : WinType.LOSE;

        return winType;
    }
}
