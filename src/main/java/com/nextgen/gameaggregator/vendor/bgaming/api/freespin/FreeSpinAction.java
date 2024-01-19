package com.nextgen.gameaggregator.vendor.bgaming.api.freespin;

import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.bgaming.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.bgaming.vo.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class FreeSpinAction {
    @Autowired
    private HttpService httpService;

    @PostMapping(path = EndPoints.FREESPINS)
    public ResponseEntity<ResponseVo> freeSpins(HttpServletRequest request) {
        /*
           TODO: This endpoint will only be triggered if our side have call request issue free spins to player.
             ( When casino receives request with statusactive or playedbut issue is not activated in casino then casino should activate issue and continue processing request. )
             To update this endpoint if our side have set up Free Spin Request Service.
             This endpoint only return 0 balance for now.
        */

        HttpRequestLog httpRequestLog = httpService.start(request);
        String traceId = httpRequestLog.getId();
        ResponseVo responseVo = new ResponseVo();
        Integer httpStatus;
        try {
            // Retrieve request body in original string format
            String body = httpRequestLog.getRequestBody();

            // Convert original request body into dto
            FreeSpinDto freeSpinDto = HttpService.convertJsonToDto(body, FreeSpinDto.class);

            // Validate the commonDto object
            this.doValidation(freeSpinDto);

            // Construct VO
            responseVo.setBalance(0);
            responseVo.setHttpStatus(HttpStatus.SC_OK);
        } catch (Exception e) {
            responseVo.setHttpStatus(HttpStatus.SC_BAD_REQUEST);
            httpService.logError(httpRequestLog, e);
        } finally {
            httpStatus = responseVo.getHttpStatus();
            responseVo.setHttpStatus(null);
            httpService.end(httpRequestLog, responseVo);
        }
        return new ResponseEntity<>(responseVo, HttpStatusCode.valueOf(httpStatus));
    }

    private void doValidation(FreeSpinDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }
}
