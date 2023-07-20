package com.nextgen.gameaggregator.vendor.playngo.api.authenticate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nextgen.gameaggregator.entity.HttpRequestLog;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.service.HttpService;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import com.nextgen.gameaggregator.vendor.playngo.constant.EndPoints;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.StringReader;

@RestController
@RequestMapping(path = EndPoints.PATH)
@Slf4j
public class AuthAction {

    @Autowired
    private HttpService httpService;

    @Autowired
    private VendorLineService vendorLineService;

    @PostMapping(path = EndPoints.AUTHTHENTICATE)
    public AuthVo balance(HttpServletRequest request) throws InvalidRequestException, JsonProcessingException {
        HttpRequestLog httpRequestLog = httpService.start(request);

        String traceId = httpRequestLog.getId();

        // Construct VO
        AuthVo authVo = new AuthVo();

        //Retrieve request body in original string format
        String body = httpRequestLog.getRequestBody();

        //Convert original request body into commonDto
        XmlMapper xmlMapper = new XmlMapper();
        AuthDto authDto = xmlMapper.readValue(body, AuthDto.class);

        //Validate request parameters from vendor (Non-database related)
        this.doValidation(authDto);
        //AuthDto authDto = (AuthDto)this.xmlToObject(body, AuthDto.class);
//        JAXBContext jaxbContext;
//        try
//        {
//            jaxbContext = JAXBContext.newInstance(AuthDto.class);
//            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
//            AuthDto authDto = (AuthDto) jaxbUnmarshaller.unmarshal(new StringReader(body));
//
//            System.out.println(authDto);
//        }
//        catch (JAXBException e)
//        {
//            e.printStackTrace();
//        }

        httpService.end(httpRequestLog, authVo);
        return authVo;
    }

    private static Object xmlToObject(String xml, Class<?> clazz) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            return unmarshaller.unmarshal(new StringReader(xml));
        } catch (JAXBException e) {
            throw new IllegalArgumentException("Error while converting xml to object", e);
        }
    }

    private void doValidation(AuthDto dto) throws InvalidRequestException {
        // General validation
        ValidationUtils.validateRequest(dto);
    }
//
//    private void doVerification(HttpRequestLog request, AuthDto dto, GameSession gameSession) throws NoAvailableLineException, CredentialNotFoundException, InvalidSignatureException {
//
//        //Verify received agent code is the same from credential
//        String agentCode = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.APP_ID);
//        ValidationUtils.isEquals(agentCode, dto.getAppid(), NoAvailableLineException::new);
//
//        //Verify received hash
//        String secretKey = vendorLineService.getCredentialValueByName(gameSession.getVendorLineId(), Credentials.SECRET);
//        VendorService.verifyHash(request.getRequestBody(), secretKey);
//
//    }

}
