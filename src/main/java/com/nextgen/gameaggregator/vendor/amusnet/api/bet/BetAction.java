package com.nextgen.gameaggregator.vendor.amusnet.api.bet;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.entity.ga.SettledBet;
import com.nextgen.gameaggregator.eventing.events.BetEvent;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.service.*;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.amusnet.constant.Credentials;
import com.nextgen.gameaggregator.vendor.amusnet.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.amusnet.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.amusnet.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.auth.InvalidCredentialsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class BetAction {
    private final HttpService httpService;
    private final GameSessionService gameSessionService;
    private final WalletService walletService;
    private final ValidationService validationService;
    private final VendorLineService vendorLineService;
    private final VendorService vendorService;
    private final SettledBetService settledBetService;

    @Autowired
    public BetAction(HttpService httpService, GameSessionService gameSessionService,
                     WalletService walletService, ValidationService validationService,
                     VendorLineService vendorLineService, VendorService vendorService,
                     SettledBetService settledBetService) {
        this.httpService = httpService;
        this.gameSessionService = gameSessionService;
        this.walletService = walletService;
        this.validationService = validationService;
        this.vendorLineService = vendorLineService;
        this.vendorService = vendorService;
        this.settledBetService = settledBetService;
    }

    @PostMapping(path = EndPoints.WITHDRAW)
    public String bet(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        BetVo vo = new BetVo();
        XmlMapper xmlMapper = new XmlMapper();
        BigDecimal balance;
        BetDto betDto = new BetDto();
        boolean isRequestExists = false;
        try {

            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            betDto = xmlMapper.readValue(body, BetDto.class);

            // Validate request parameters from vendor (Non-database calls)
            this.doValidation(betDto);

            // Verify session token
            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(betDto.getPlayerId());
            gameSession = vendorService.verifyAndRegenerateNewVendorGameCodeForGameSession(betDto.getVendorGameId(), gameSession);

            // Verify remaining parameters (Verify against database values)
            this.doVerification(betDto, gameSession);
            
            BetEvent betEvent = walletService.processBet
                    (httpRequestLog.getId(), gameSession, betDto, httpRequestLog.getRequestBody(), httpRequestLog);
            balance = betEvent.getLastBalance();
            // Set response data
            vo.setBalance(balance.toBigInteger());
            vo.setCasinoTransferId(httpRequestLog.getId());
            vo.setResponseCodes(ResponseCodes.OK);

        } catch (BetResultIdempotentViolationException e) {
            httpService.logError(httpRequestLog, e);
            vo.setResponseCodes(ResponseCodes.DUPLICATE);
            vo.setBalance(e.getBalance().toBigInteger());

        } catch (InsufficientBalanceException e) {
            httpService.logError(httpRequestLog, e);
            vo.setResponseCodes(ResponseCodes.INSUFFICIENT_FUNDS);

        } catch (InvalidOperatorResponseException e) {
            if (e.getOperatorStatus().equals(com.nextgen.gameaggregator.operator.constant.ResponseCodes.Status.SC_INSUFFICIENT_FUNDS.code)) {
                httpService.logError(httpRequestLog, e);
                vo.setResponseCodes(ResponseCodes.INSUFFICIENT_FUNDS);
            } else {
                httpService.logError(httpRequestLog, e);
                vo.setResponseCodes(ResponseCodes.INTERNAL_SERVER_ERROR);
            }
        } catch (TransactionStillProcessingException e) {
            httpService.logError(httpRequestLog, e);
            vo.setResponseCodes(ResponseCodes.TIME_OUT);

        } catch (Exception e) { // any other exception encountered
            httpService.logError(httpRequestLog, e);
            vo.setResponseCodes(ResponseCodes.INTERNAL_SERVER_ERROR);

        } finally {
            vendorService.buildResponseVo(vo);
            httpService.end(httpRequestLog, vo);
        }

        return vo.getResponseXMLFormat();
    }

    private void doValidation(BetDto betDto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(betDto);
    }

    private void doVerification(BetDto betDto, GameSession gameSession) throws
            AuthenticationException,
            DisabledVendorLineException,
            DisabledAgentPlayerException,
            DisabledGameException,
            InvalidRequestException,
            CredentialNotFoundException,
            InvalidPlayerException,
            InvalidCredentialsException,
            CurrencyNotSupportedException,
            BetResultIdempotentViolationException {

        ///validate vendor username, agent vendor line, player status, and game status
        validationService.validateEligibleBet(gameSession, betDto.getPlayerId());

        this.verifySettledBet(betDto, gameSession);
        // Verify vendor currency
        ValidationUtils.isEquals(gameSession.getVendorCurrencyCode(), betDto.getCurrency(), CurrencyNotSupportedException::new);

        // Verify username
        ValidationUtils.isEquals(gameSession.getVendorPlayerUsername(), betDto.getPlayerId(), AuthenticationException::new);

        String userName = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.USERNAME);
        String password = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.PASSWORD);
        String portalCodeEQ = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.PORTAL_CODE_EQ);
        String portalCode = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.PORTAL_CODE);
        String categoryCodeList = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.CATEGORY_CODE_EQ);
        String verifiedPortalCode = vendorService.checkGameCodeIsOpenEQGame(categoryCodeList, betDto.getVendorGameId(), portalCodeEQ, portalCode);

        ValidationUtils.isEquals(userName, betDto.getUserName(), InvalidCredentialsException::new);
        ValidationUtils.isEquals(password, betDto.getPassword());
        ValidationUtils.isEquals(verifiedPortalCode, betDto.getPortalCode());

    }

    private void verifySettledBet(BetDto dto, GameSession gameSession) throws BetResultIdempotentViolationException {
        List<SettledBet> settledBetList = settledBetService.getByVendorPlayerIdAndRoundId(gameSession.getVendorPlayerId(), dto.getGameNumber());

        if (!settledBetList.isEmpty() && settledBetList.get(0).getOperatorStatus().equals(1)) {
            throw new BetResultIdempotentViolationException(settledBetList.get(0));
        }
    }
}
