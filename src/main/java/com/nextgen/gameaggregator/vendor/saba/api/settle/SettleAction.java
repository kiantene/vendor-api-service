package com.nextgen.gameaggregator.vendor.saba.api.settle;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.vendor.saba.constant.EndPoints;
import com.nextgen.gameaggregator.vendor.saba.dto.RequestDto;
import com.nextgen.gameaggregator.vendor.saba.service.GzipUtils;
import com.nextgen.gameaggregator.vendor.saba.vo.GeneralVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class SettleAction {

    @Autowired
    private HttpService httpService;

    @PostMapping(path = EndPoints.SETTLE)
    public GeneralVo action(@RequestBody byte[] request) throws IOException {
        String decompressedRequestBody = GzipUtils.decompress(request);

        Map<String, String> requestLog = new HashMap<>();
        requestLog.put("Title", "SABA Testing");
        requestLog.put("Decompressed Request Body", decompressedRequestBody);
        log.info(requestLog.toString());


//        HttpRequestLog httpRequestLog = httpService.start(request);
//        String traceId = httpRequestLog.getId();

        // Construct Vo
        GeneralVo vo = new GeneralVo();

        try {
            // Convert original request body into dto
            RequestDto<SettleDto> dto = HttpService.convertJsonToDto(decompressedRequestBody, new TypeReference<>() {
            });

            vo.setStatus("0");

        } catch (Exception e) {
            System.out.println(e.getMessage());

        }

        return vo;
    }
}
