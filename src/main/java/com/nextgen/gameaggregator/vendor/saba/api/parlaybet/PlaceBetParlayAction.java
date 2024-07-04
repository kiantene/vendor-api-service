package com.nextgen.gameaggregator.vendor.saba.api.parlaybet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.core.WalletRequestService;
import com.nextgen.gameaggregator.entity.ga.GameSession;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.enums.BetStatus;
import com.nextgen.gameaggregator.enums.BetType;
import com.nextgen.gameaggregator.exception.BetResultIdempotentViolationException;
import com.nextgen.gameaggregator.exception.InsufficientBalanceException;
import com.nextgen.gameaggregator.operator.dto.MultipleBetDto;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.sport.service.SportWalletService;
import com.nextgen.gameaggregator.vendor.saba.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.saba.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.saba.dto.RequestDto;
import com.nextgen.gameaggregator.vendor.saba.service.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class PlaceBetParlayAction {

    private final GameSessionService gameSessionService;
    private final HttpService httpService;
    private final SportWalletService sportWalletService;
    private final WalletRequestService walletRequestService;

    @Autowired
    public PlaceBetParlayAction(GameSessionService gameSessionService,
                                HttpService httpService,
                                SportWalletService sportWalletService,
                                WalletRequestService walletRequestService) {

        this.gameSessionService = gameSessionService;
        this.httpService = httpService;
        this.sportWalletService = sportWalletService;
        this.walletRequestService = walletRequestService;
    }

    @PostMapping(path = EndPoints.PLACE_BET_PARLAY)
    public PlaceBetParlayVo action(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        WalletRequest walletRequest = WalletRequestService.init(httpRequestLog);

        // Construct Vo
        PlaceBetParlayVo vo = new PlaceBetParlayVo();

        try {
            // Convert original request body into dto
            RequestDto<PlaceBetParlayDto> dtos = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), new TypeReference<>() {
            });

            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dtos.getMessage().getUserId());
            walletRequest = walletRequestService.updateByGameSession(walletRequest, gameSession);

            this.dataMapper(walletRequest, dtos.getMessage());

            if (this.isMultipleBet(dtos.getMessage())) {
                walletRequest = sportWalletService.placeMultipleBets(walletRequest);
            } else {
                walletRequest = sportWalletService.placeBet(walletRequest);
            }

            this.buildResponseVo(walletRequest, vo);

        } catch (BetResultIdempotentViolationException e) {
            vo.setResponseCode(ResponseCode.DUPLICATE_TRANSACTION);
            walletRequest.setErrorMessage(e.getMessage());

        } catch (InsufficientBalanceException e) {
            vo.setResponseCode(ResponseCode.INSUFFICIENT_BALANCE);
            walletRequest.setErrorMessage(e.getMessage());

        } catch (Exception e) {
            vo.setResponseCode(ResponseCode.SYSTEM_ERROR_RETRY);
            httpService.logError(httpRequestLog, e);
            walletRequest.setErrorMessage(e.getMessage());

        } finally {
            walletRequestService.end(walletRequest, httpRequestLog, vo);

        }

        return vo;
    }

    private void dataMapper(WalletRequest walletRequest, PlaceBetParlayDto placeBetParlayDto) {
        String operationId = placeBetParlayDto.getOperationId();
        String refId = placeBetParlayDto.getTxns().get(0).getRefId();
        walletRequest.setExternalTransactionId(operationId);
        walletRequest.setVendorBetId(refId);
        walletRequest.setRoundId(refId);
        walletRequest.setBetAmount(placeBetParlayDto.getTotalBetAmount());
        walletRequest.setVendorBetTime(System.currentTimeMillis());
        walletRequest.setBetStatus(BetStatus.UNSETTLED);
        walletRequest.setBetType(BetType.PARLAY_BET.code);
        walletRequest.setVendorPlayerUsername(placeBetParlayDto.getUserId());

        if (isMultipleBet(placeBetParlayDto)) {
            List<String> refIdList = new LinkedList<>();
            List<MultipleBetDto> multipleBetList = new ArrayList<>();
            for (PlaceBetParlayTxnsDto placeBetParlayTxnsDto : placeBetParlayDto.getTxns()) {
                String externalTransactionId = VendorService.generateExtTxnId(operationId, placeBetParlayTxnsDto.getRefId());
                MultipleBetDto multipleBetDto = new MultipleBetDto();
                multipleBetDto.setVendorBetId(placeBetParlayTxnsDto.getRefId());
                multipleBetDto.setExternalTransactionId(externalTransactionId);
                multipleBetDto.setBetAmount(placeBetParlayTxnsDto.getBetAmount());
                multipleBetList.add(multipleBetDto);
                refIdList.add(placeBetParlayTxnsDto.getRefId());
            }
            // generate md5 vendorBetId and roundId for masterUnsettleBet using joinedRefId
            String md5RefId = VendorService.generateMultipleBetRoundId(refIdList);
            walletRequest.setVendorBetId(md5RefId);
            walletRequest.setRoundId(md5RefId);
            walletRequest.setBetIds(multipleBetList);
        } else {
            walletRequest.setExternalTransactionId(VendorService.generateExtTxnId(operationId, refId));
        }
    }

    private boolean isMultipleBet(PlaceBetParlayDto placeBetParlayDto) {
        return placeBetParlayDto.getTxns().size() > 1;
    }

    private void buildResponseVo(WalletRequest walletRequest, PlaceBetParlayVo vo) {
        List<PlaceBetParlayTxnsVo> txnsVoList = new ArrayList<>();

        if (Objects.isNull(walletRequest.getBetIds()) || walletRequest.getBetIds().isEmpty()) {
            PlaceBetParlayTxnsVo placeBetParlayTxnsVo = new PlaceBetParlayTxnsVo();
            placeBetParlayTxnsVo.setRefId(walletRequest.getVendorBetId());
            placeBetParlayTxnsVo.setLicenseeTxId(walletRequest.getBetId());
            txnsVoList.add(placeBetParlayTxnsVo);

        } else {
            walletRequest.getBetIds().forEach(multipleBetDto -> {
                PlaceBetParlayTxnsVo placeBetParlayTxnsVo = new PlaceBetParlayTxnsVo();
                placeBetParlayTxnsVo.setRefId(multipleBetDto.getVendorBetId());
                placeBetParlayTxnsVo.setLicenseeTxId(multipleBetDto.getBetId());
                txnsVoList.add(placeBetParlayTxnsVo);
            });
        }

        vo.setTxns(txnsVoList);
        vo.setResponseCode(ResponseCode.SUCCESS);
    }
}
