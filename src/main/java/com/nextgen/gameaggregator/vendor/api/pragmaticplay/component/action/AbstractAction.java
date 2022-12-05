package com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.vendor.data.couchbase.config.entity.*;
import com.nextgen.gameaggregator.vendor.data.mariadb.reader.entity.AgentCredentialReader;
import com.nextgen.gameaggregator.vendor.data.mariadb.reader.entity.VendorReader;
import com.nextgen.gameaggregator.vendor.data.mariadb.reader.manager.AgentCredentialReaderManager;

import com.nextgen.gameaggregator.vendor.data.mariadb.reader.manager.VendorReaderManager;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AbstractAction {

    @Autowired
    private VendorReaderManager vendorReaderManager;
    private VendorReader vendorReader;

    @Autowired
    private AgentCredentialReaderManager agentCredentialReaderManager;
    private AgentCredentialReader agentCredentialReader;

    @Autowired
    private SeamlessActionLogRepository seamlessActionLogRepository;

    @Autowired
    private SeamlessRefundLogRequestRepository seamlessRefundLogRequestRepository;

    @Autowired
    private VendorPlayerAuthenticationRepository vendorPlayerAuthenticationRepository;

    private VendorPlayerAuthentication vendorPlayerAuthentication = new VendorPlayerAuthentication();

    public <T> T queryStringToDto(String queryString, Class<T> clazz) {

        System.out.println(queryString);

        HashMap<String, Object> queryParameterMap = new HashMap<String, Object>();
        String[] fields = queryString.split("&");

        for (int i = 0; i < fields.length; ++i) {
            String[] kv = fields[i].split("=");
            if (2 == kv.length) {
                queryParameterMap.put(kv[0], kv[1]);
            }
        }

        ObjectMapper mapper = new ObjectMapper();
        T t = mapper.convertValue(queryParameterMap, clazz);

        return t;
    }

    public <T> Map<String, String> doValidation(T dto, Class<T> clazz) {
        Map<String, String> validationMap = new HashMap<String, String>();

        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<T>> violations = validator.validate(dto);
        for (ConstraintViolation<T> violation : violations) {
            validationMap.put(violation.getPropertyPath().toString(), violation.getPropertyPath() + " " + violation.getMessage());
        }
        return validationMap;
    }

    //region handle query string data to map object
    public HashMap<String, Object> handleQueryStringDataToMapObject(String queryString){

        HashMap<String, Object> things = new HashMap<String, Object>();
        String[] fields = queryString.split("&");
        String[] kv = new String[0];

        for (int i = 0; i < fields.length; ++i)
        {
            kv = fields[i].split("=");
            if (2 == kv.length)
            {
                things.put(kv[0], kv[1]);
            }
        }

        return things;
    }
    //endregion

    //region find class file by vendor_code in vendor table
    public String findClassFileByVendorCode (String vendorCode){
        vendorReader = vendorReaderManager.findByVendorCode(vendorCode);

        return vendorReader.getClassFile();
    }
    //endregion

    //region find agent credential id by agent_id in agent_credentials table
    public Long findAgentCredentialIdByAgentId (Long agentId){
        agentCredentialReader = agentCredentialReaderManager.findByAgentId(agentId);

        return agentCredentialReader.getId();
    }
    //endregion

    //region create seamless bet result log into couchbase log.seamless_result_log table
    public void createSeamlessResultLogRecord (String id, Long aggregatorRequestStartMs, String rawRequest){
        SeamlessActionLogRequest dataSet = new SeamlessActionLogRequest(id, aggregatorRequestStartMs, rawRequest);
        this.seamlessActionLogRepository.save(dataSet);
    }
    //endregion

    //region insert seamless_refund_log on couchbase
    public void createSeamlessRefundLogRecord(String vendorBetId, String vendorCode, String status, Long aggregatorRequestStartMs,
                                              String rawRequest){

        SeamlessRefundLogRequest dataSet = new SeamlessRefundLogRequest(vendorBetId, vendorCode, status,
                aggregatorRequestStartMs, rawRequest);

        this.seamlessRefundLogRequestRepository.save(dataSet);

    }
    //endregion

    //region match the trace id (token) from vendor and vendor_player_authentication table then get all data
    public VendorPlayerAuthentication findTraceId(String traceId){
        vendorPlayerAuthentication = vendorPlayerAuthenticationRepository.findByTraceId(traceId);

        return vendorPlayerAuthentication;
    }
    //endregion
}
