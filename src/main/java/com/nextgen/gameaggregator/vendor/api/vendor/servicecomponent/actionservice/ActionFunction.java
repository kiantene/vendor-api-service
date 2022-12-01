package com.nextgen.gameaggregator.vendor.api.vendor.servicecomponent.actionservice;

import com.nextgen.gameaggregator.vendor.data.couchbase.config.entity.SeamlessActionLogRepository;
import com.nextgen.gameaggregator.vendor.data.couchbase.config.entity.SeamlessActionLogRequest;
import com.nextgen.gameaggregator.vendor.data.mariadb.reader.entity.AgentCredentialReader;
import com.nextgen.gameaggregator.vendor.data.mariadb.reader.entity.VendorReader;
import com.nextgen.gameaggregator.vendor.data.mariadb.reader.manager.AgentCredentialReaderManager;
import com.nextgen.gameaggregator.vendor.data.mariadb.reader.manager.VendorReaderManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;

public class ActionFunction {

    @Autowired
    private VendorReaderManager vendorReaderManager;
    private VendorReader vendorReader;

    @Autowired
    private AgentCredentialReaderManager agentCredentialReaderManager;
    private AgentCredentialReader agentCredentialReader;

    @Autowired
    private SeamlessActionLogRepository seamlessActionLogRepository;

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
}
