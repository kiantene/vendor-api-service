package com.nextgen.gameaggregator.vendor.saba.api.parlaybet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nextgen.gameaggregator.entity.GameSession;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.BetResultIdempotentViolationException;
import com.nextgen.gameaggregator.service.GameSessionService;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.sport.service.SportWalletService;
import com.nextgen.gameaggregator.vendor.saba.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.saba.constant.ResponseCode;
import com.nextgen.gameaggregator.vendor.saba.dto.RequestDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class PlaceBetParlayAction {

    @Autowired
    private GameSessionService gameSessionService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private SportWalletService sportWalletService;

    @PostMapping(path = EndPoints.PLACE_BET_PARLAY)
    public PlaceBetParlayVo action(HttpServletRequest request) {

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();

        // Construct Vo
        PlaceBetParlayVo vo = new PlaceBetParlayVo();
        List<PlaceBetParlayTxnsVo> txnsVoList = new ArrayList<>();

        try {
            // Convert original request body into dto
            RequestDto<PlaceBetParlayDto> dtos = HttpService.convertJsonToDto(httpRequestLog.getRequestBody(), new TypeReference<>() {
            });

            GameSession gameSession = gameSessionService.getGameSessionByVendorPlayerUsername(dtos.getMessage().getUserId());


            for (PlaceBetParlayTxnsDto txnsDto : dtos.getMessage().getTxns()) {
                PlaceBetParlayTxnsVo txnsVo = new PlaceBetParlayTxnsVo();

                dtos.getMessage().setRefId(txnsDto.getRefId());
                dtos.getMessage().setBetAmount(txnsDto.getBetAmount());

                String betId = UUID.randomUUID().toString();
                sportWalletService.placeBet(betId, gameSession, dtos.getMessage(), httpRequestLog.getRequestBody(), httpRequestLog);

                txnsVo.setRefId(txnsDto.getRefId());
                txnsVo.setLicenseeTxId(betId);

                txnsVoList.add(txnsVo);
            }

            vo.setTxns(txnsVoList);
            vo.setResponseCode(ResponseCode.SUCCESS);

        } catch (BetResultIdempotentViolationException e) {
            vo.setResponseCode(ResponseCode.DUPLICATE_TRANSACTION);

        } catch (Exception e) {
            vo.setResponseCode(ResponseCode.SYSTEM_ERROR_RETRY);
            httpService.logError(httpRequestLog, e);

        } finally {
            httpService.end(httpRequestLog, vo);

        }

        return vo;
    }
}
