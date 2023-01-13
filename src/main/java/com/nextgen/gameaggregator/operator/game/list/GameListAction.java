package com.nextgen.gameaggregator.operator.game.list;


import com.nextgen.gameaggregator.entity.AgentApiCredential;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.exception.RecordNotFoundException;
import com.nextgen.gameaggregator.operator.constant.Endpoints;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import com.nextgen.gameaggregator.operator.vo.OperatorResponseVo;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.ValidationService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(path = "game/")
@Slf4j
public class GameListAction {
    @Autowired
    private HttpService httpService;

    @Autowired
    private ValidationService validationService;

    @Autowired
    private GameListService gameListService;

    @PostMapping(path = "list")
    public OperatorResponseVo<GameListData> list(HttpServletRequest request) {
        HttpRequestLog httpRequestLog = httpService.start(request);
        OperatorResponseVo<GameListData> responseVo = new OperatorResponseVo<>();
        try {
            // Retrieve request body in original string format and convert into dto
            String body = httpRequestLog.getRequestBody();
            GameListDto dto = HttpService.convertJsonToDto(body, GameListDto.class);

            responseVo.setTraceId(dto.getTraceId());
            httpRequestLog.setTraceId(dto.getTraceId());
            log.info(dto.toString());

            // 1. Validate all fields in the request object
            ValidationUtils.validateRequest(dto);

            // 2. Check if api key is valid
            String apiKey = request.getHeader(Endpoints.HEADER_API_KEY);
            //TODO (by Alex), check agent status
            AgentApiCredential apiCredential = validationService.validateApiKey(apiKey);

            //TODO (bu Alex), to discuss should validated agent supported vendor
            GameListData gameListData = gameListService.getGameList(dto);
            responseVo.setData(gameListData);

        } catch (IllegalArgumentException illegalArgumentException) {
            // thrown when any field encountered type mismatch during conversion from json to dto
            log.error(illegalArgumentException.toString());
            responseVo.setStatus(ResponseCodes.Status.SC_MISMATCHED_DATA_TYPE);

        } catch (InvalidRequestException invalidRequestException) {
            responseVo.setStatus(ResponseCodes.Status.SC_INVALID_REQUEST);
            responseVo.setValidation(invalidRequestException.getValidation());

        } catch (RecordNotFoundException recordNotFoundException) {
            responseVo.setStatus(ResponseCodes.Status.SC_INVALID_REQUEST);

        } catch (Exception exception) {
            responseVo.setStatus(ResponseCodes.Status.SC_UNKNOWN_ERROR);
            httpService.logError(httpRequestLog, exception);
            exception.printStackTrace();

        } finally {
            responseVo.setMessage(responseVo.getStatus().description);
        }
        httpService.end(httpRequestLog, responseVo);
        return responseVo;

    }

}
