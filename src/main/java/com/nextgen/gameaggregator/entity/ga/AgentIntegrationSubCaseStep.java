package com.nextgen.gameaggregator.entity.ga;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Entity
@Table(name = "agent_integration_sub_case_steps", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"masterCaseId", "subCaseId", "stepId", "agentId"})
})

//@Table(name = "agent_integration_sub_case_steps")
@Data
public class AgentIntegrationSubCaseStep {
    @Id
    private Integer id;
    private Integer agentId;
    private String caseName;
    private String description;
    private Integer masterCaseId;
    private Integer subCaseId;
    private Integer stepId;
    private Long startTime;
    private Long endTime;
    private String apiUrl;
    private String apiEndpoint;
    private String requestHeaders;
    private String requestBody;
    private Integer responseHttpCode;
    private String responseBody;
    private String expectedResponse;
    private Integer status;
    private String messageCode;
    private String remark;
    private Integer createById;
    private String createByUsertype;
    private String createByIp;
    private Long createDate;

    private Integer updateById;
    private String updateByUsertype;
    private String updateByIp;
    private Long updateDate;

    public AgentIntegrationSubCaseStep(Integer agentId, String caseName, String description, Integer masterCaseId, Integer subCaseId, Integer stepId, String apiEndpoint) {
        this.agentId = agentId;
        this.caseName = caseName;
        this.description = description;
        this.masterCaseId = masterCaseId;
        this.subCaseId = subCaseId;
        this.stepId = stepId;
        this.startTime = null;
        this.endTime = null;
        this.apiUrl = "";
        this.apiEndpoint = apiEndpoint;
        this.requestHeaders = "";
        this.requestBody = "";
        this.responseHttpCode = 0;
        this.responseBody = "";
        this.expectedResponse = "";
        this.status = 2;
        this.messageCode = "INPROGRESS";
        this.remark = "";

        String ip = "Unknown";
        try {
            // This exception should not block the saving of new records
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (
                UnknownHostException unknownHostException) {
            unknownHostException.printStackTrace();
        }
        this.createById = 0;
        this.createByUsertype = "operator-api-service";
        this.createByIp = ip;
        this.createDate = System.currentTimeMillis();

        this.updateById = null;
        this.updateByUsertype = null;
        this.updateByIp = null;
        this.updateDate = null;
    }

    public AgentIntegrationSubCaseStep() {

    }
}
