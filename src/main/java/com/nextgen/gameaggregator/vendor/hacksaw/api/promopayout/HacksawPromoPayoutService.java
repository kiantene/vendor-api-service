package com.nextgen.gameaggregator.vendor.hacksaw.api.promopayout;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nextgen.gameaggregator.core.exception.DuplicateRequestException;
import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.hacksaw.api.endround.CreditDto;
import com.nextgen.gameaggregator.vendor.hacksaw.constant.ResponseCodes;
import com.nextgen.gameaggregator.vendor.hacksaw.vo.ResponseVo;
import org.springframework.stereotype.Service;

@Service
public class HacksawPromoPayoutService {

    private final HacksawPromoPayoutHandler handler;
    private final HttpService httpService;

    public HacksawPromoPayoutService(HacksawPromoPayoutHandler handler, HttpService httpService) {
        this.handler = handler;
        this.httpService = httpService;
    }

    public ResponseVo handle(HttpRequestLog httpRequestLog) {
        ResponseVo vo = new ResponseVo();
        try {
            String body = httpRequestLog.getRequestBody();
            PromoPayoutDto dto = HttpService.convertJsonToDto(body, PromoPayoutDto.class);
            ValidationUtils.validateRequest(dto);

            vo = handler.process(dto);

        } catch (JsonProcessingException | InvalidRequestException e) {
            vo.setResponseCodes(ResponseCodes.INVALID_ACTION);
            httpService.logError(httpRequestLog, e);

        } catch (DuplicateRequestException e) {
            httpService.logError(httpRequestLog, e);
            if (e.getCurrency() == null) {
                vo.setResponseCodes(ResponseCodes.GENERAL_ERROR);
            } else {
                vo.setAccountBalance(e.getBalance().longValue());
                vo.setExternalTransactionId(e.getTransactionId());
            }

        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            vo.setResponseCodes(ResponseCodes.GENERAL_ERROR);
        }
        return vo;
    }

    public ResponseVo handleFromCredit(CreditDto creditDto, HttpRequestLog httpRequestLog) {
        ResponseVo vo = new ResponseVo();
        try {
            PromoPayoutDto dto = new PromoPayoutDto();
            dto.setAction(creditDto.getAction());
            dto.setSecret(creditDto.getSecret());
            dto.setExternalPlayerId(creditDto.getExternalPlayerId());
            dto.setPromoPayoutId(creditDto.getTransactionId());
            dto.setPromotionId(creditDto.getFreeRoundData().getFreeRoundActivationId());
            dto.setExternalPromoId(creditDto.getFreeRoundData().getExternalId());
            dto.setAmount(creditDto.getAmount());
            dto.setCurrency(creditDto.getCurrency());

            ValidationUtils.validateRequest(dto);
            vo = handler.process(dto);

        } catch (InvalidRequestException e) {
            vo.setResponseCodes(ResponseCodes.INVALID_ACTION);
            httpService.logError(httpRequestLog, e);

        } catch (DuplicateRequestException e) {
            httpService.logError(httpRequestLog, e);
            if (e.getCurrency() == null) {
                vo.setResponseCodes(ResponseCodes.GENERAL_ERROR);
            } else {
                vo.setAccountBalance(e.getBalance().longValue());
                vo.setExternalTransactionId(e.getTransactionId());
            }

        } catch (Exception e) {
            httpService.logError(httpRequestLog, e);
            vo.setResponseCodes(ResponseCodes.GENERAL_ERROR);
        }
        return vo;
    }
}
