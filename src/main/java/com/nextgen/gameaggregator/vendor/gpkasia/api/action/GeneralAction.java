package com.nextgen.gameaggregator.vendor.gpkasia.api.action;

import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.gpkasia.dto.ActionDto;
import com.nextgen.gameaggregator.vendor.gpkasia.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.gpkasia.vo.CommonVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class GeneralAction {

    @Autowired
    private HttpService httpService;


    @PostMapping(path = EndPoints.ACTION)
    public CommonVo action(HttpServletRequest request){
        HttpRequestLog httpRequestLog = httpService.start(request);
        httpRequestLog.setRequestBody(request.getQueryString());
        String traceId = httpRequestLog.getId();

        CommonVo vo = new CommonVo();

        try{

        }finally{
            httpService.end(httpRequestLog, vo);
        }

        return vo;
    }

    private void doValidation(ActionDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }
}
