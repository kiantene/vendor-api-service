package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.AgentIntegrationSubCaseStep;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentIntegrationSubCaseStepRepository extends JpaRepository<AgentIntegrationSubCaseStep, Integer> {

    AgentIntegrationSubCaseStep findByAgentIdAndMasterCaseIdAndSubCaseIdAndStepId(Integer agentId, Integer masterCaseId, Integer subCaseId, Integer stepId);

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO agent_integration_sub_case_steps " +
            "(agent_id, case_name, master_case_id, sub_case_id, step_id, start_time, end_time, api_url, api_endpoint, " +
            "request_headers, request_body, response_http_code, response_body, expected_response, status, message_code, " +
            "remark, create_by_id, create_by_usertype, create_by_ip, create_date, update_by_id, update_by_usertype, update_by_ip, update_date) " +
            "VALUES " +
            "(:agentId, :caseName, :masterCaseId, :subCaseId, :stepId, :startTime, :endTime, :apiUrl, :apiEndpoint, " +
            ":requestHeaders, :requestBody, :responseHttpCode, :responseBody, :expectedResponse, :status, :messageCode, " +
            ":remark, :createById, :createByUsertype, :createByIp, :createDate, :updateById, :updateByUsertype, :updateByIp, :updateDate) " +
            "ON DUPLICATE KEY " +
            "UPDATE start_time = :startTime, end_time = :endTime, api_url = :apiUrl, request_headers = :requestHeaders, request_body =:requestBody, " +
            "response_http_code = :responseHttpCode, response_body=:responseBody, expected_response=:expectedResponse, status=:status, " +
            "message_code=:messageCode, remark=:remark, create_by_id=:createByUsertype, create_by_ip=:createByIp, create_date=:createDate, " +
            "update_by_id=:updateById, update_by_usertype=:updateByUsertype, update_by_ip=:updateByIp, update_date=:updateDate", nativeQuery = true)
    void insertOrUpdate(@Param("agentId") Integer agentId, @Param("caseName") String caseName, @Param("masterCaseId") Integer masterCaseId,
                        @Param("subCaseId") Integer subCaseId, @Param("stepId") Integer stepId, @Param("startTime") Long startTime,
                        @Param("endTime") Long endTime, @Param("apiUrl") String apiUrl, @Param("apiEndpoint") String apiEndpoint, @Param("requestHeaders") String requestHeaders,
                        @Param("requestBody") String requestBody, @Param("responseHttpCode") Integer responseHttpCode, @Param("responseBody") String responseBody,
                        @Param("expectedResponse") String expectedResponse, @Param("status") Integer status, @Param("messageCode") String messageCode,
                        @Param("remark") String remark, @Param("createById") Integer createById, @Param("createByUsertype") String createByUsertype,
                        @Param("createByIp") String createByIp, @Param("createDate") Long createDate, @Param("updateById") Integer updateById,
                        @Param("updateByUsertype") String updateByUsertype, @Param("updateByIp") String updateByIp,
                        @Param("updateDate") Long updateDate
                        );

    default void insertOrUpdate(List<AgentIntegrationSubCaseStep> entities) {
        for (AgentIntegrationSubCaseStep entity : entities) {
            insertOrUpdate(entity.getAgentId(), entity.getCaseName(), entity.getMasterCaseId(),
                    entity.getSubCaseId(), entity.getStepId(), entity.getStartTime(),
                    entity.getEndTime(), entity.getApiUrl(), entity.getApiEndpoint(), entity.getRequestHeaders(),
                    entity.getRequestBody(), entity.getResponseHttpCode(), entity.getResponseBody(),
                    entity.getExpectedResponse(), entity.getStatus(), entity.getMessageCode(),
                    entity.getRemark(), entity.getCreateById(), entity.getCreateByUsertype(),
                    entity.getCreateByIp(),entity.getCreateDate(), entity.getUpdateById(),
                    entity.getUpdateByUsertype(), entity.getUpdateByIp(),
                    entity.getUpdateDate()
            );
        }
    }
}
