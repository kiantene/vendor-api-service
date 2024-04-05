package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.AgentIntegrationDetail;
import com.nextgen.gameaggregator.entity.ga.AgentIntegrationSubCaseStep;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.RecordNotFoundException;
import com.nextgen.gameaggregator.operator.apiverification.agentinfo.AgentInfoVo;
import com.nextgen.gameaggregator.operator.apiverification.agenttestreport.update.AgentTestCaseStepUpdateDto;
import com.nextgen.gameaggregator.repository.ga.writer.AgentIntegrationDetailRepository;
import com.nextgen.gameaggregator.repository.ga.writer.AgentIntegrationSubCaseStepRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AgentIntegrationService {

    @Autowired
    private AgentIntegrationDetailRepository agentIntegrationDetailRepository;

    @Autowired
    private AgentIntegrationSubCaseStepRepository agentIntegrationSubCaseStepRepository;

    private static final String USERTYPE = "operator-api-service";

    public AgentInfoVo getAgentIntegrationDetails(Integer agentId, AgentInfoVo agentInfoVo) throws RecordNotFoundException {

        AgentIntegrationDetail agentIntegrationDetail = agentIntegrationDetailRepository.findById(agentId).orElseThrow(RecordNotFoundException::new);
        agentInfoVo.setUsername(agentIntegrationDetail.getUsername());
        agentInfoVo.setGameCode(agentIntegrationDetail.getGameCode());
        return agentInfoVo;
    }

    public void updateAgentIntegrationDetails(Integer agentId, String username, String gameCode) {
        AgentIntegrationDetail agentIntegrationDetail = new AgentIntegrationDetail();
        agentIntegrationDetail.setId(agentId);
        agentIntegrationDetail.setUsername(username);
        agentIntegrationDetail.setGameCode(gameCode);
        agentIntegrationDetail.setStatus(Status.ACTIVE.code);
        agentIntegrationDetail.prepareSave(0, USERTYPE);
        agentIntegrationDetailRepository.save(agentIntegrationDetail);

    }

    public void CreateAndFlushNewAgentStepReport(Integer agentId) {

        List<AgentIntegrationSubCaseStep> stepCases = new ArrayList<>();

        //region Validate Invalid Signature
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "", "",1, 1, 1, "wallet/balance"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 1, 2, 1, "wallet/bet"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 1, 3, 1, "wallet/bet_result"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 1, 4, 1, "wallet/rollback"));
        //endregion

        //region Validate Invalid Username
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 2, 1, 1, "wallet/balance"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 2, 2, 1, "wallet/bet"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 2, 3, 1, "wallet/bet_result"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 2, 4, 1, "wallet/rollback"));
        //endregion

        //region Validate Insufficient Balance
        //Validate Insufficient Balance, Insufficient Balance wallet/bet
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 3, 1, 1, "wallet/balance"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 3, 1, 2, "wallet/bet"));
        //Validate Insufficient Balance, Insufficient Balance wallet/bet_result "BET_WIN"
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 3, 2, 1, "wallet/balance"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 3, 2, 2, "wallet/bet_result"));
        //Validate Insufficient Balance, Insufficient Balance wallet/bet_result "BET_LOSE"
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 3, 3, 1, "wallet/balance"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 3, 3, 2, "wallet/bet_result"));
        //endregion

        //region Validate Debit Amount
        //Validate Debit Amount wallet/bet then "END"
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 4, 1, 1, "wallet/balance"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 4, 1, 2, "wallet/bet"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 4, 1, 3, "wallet/bet_result"));
        //Validate Debit Amount wallet/bet_result "BET_LOSE"
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 4, 2, 1, "wallet/balance"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 4, 2, 2, "wallet/bet_result"));
        //endregion

        //region Validate Credit Win Amount
        //Validate Credit Amount wallet/bet_result "WIN"
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 5, 1, 1, "wallet/balance"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 5, 1, 2, "wallet/bet"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 5, 1, 3, "wallet/bet_result"));
        //Validate Credit Amount wallet/bet_result "WIN" then "END"
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 5, 2, 1, "wallet/balance"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 5, 2, 2, "wallet/bet"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 5, 2, 3, "wallet/bet_result"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 5, 2, 4, "wallet/bet_result"));
        //Validate Credit Amount  wallet/bet_result "BET_WIN"
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 5, 3, 1, "wallet/balance"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 5, 3, 2, "wallet/bet_result"));
        //Validate Credit Amount  wallet/bet_result "BET_WIN" with Zero betAmount
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 5, 4, 1, "wallet/balance"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 5, 4, 2, "wallet/bet_result"));
        //Validate Credit Amount wallet/bet_result "BET_WIN" then "END"
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 5, 5, 1, "wallet/balance"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 5, 5, 2, "wallet/bet_result"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 5, 5, 3, "wallet/bet_result"));
        //Validate Credit Amount wallet/bet_result "BET_WIN" then "WIN" then End
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 5, 6, 1, "wallet/balance"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 5, 6, 2, "wallet/bet_result"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 5, 6, 3, "wallet/bet_result"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 5, 6, 4, "wallet/bet_result"));
        //endregion

        //region Validate Credit Jackpot Amount
        //Validate Credit Amount  wallet/bet_result "WIN" with Jackpot only
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 6, 1, 1, "wallet/balance"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 6, 1, 2, "wallet/bet"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 6, 1, 3, "wallet/bet_result"));
        //Validate Credit Amount wallet/bet_result "WIN" with Jackpot only then "END"
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 6, 2, 1, "wallet/balance"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 6, 2, 2, "wallet/bet"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 6, 2, 3, "wallet/bet_result"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 6, 2, 4, "wallet/bet_result"));
        //Validate Credit Amount  wallet/bet_result "BET_WIN" with Jackpot only
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 6, 3, 1, "wallet/balance"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 6, 3, 2, "wallet/bet_result"));
        //Validate Credit Amount wallet/bet_result "BET_WIN" with Jackpot only then "END"
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 6, 4, 1, "wallet/balance"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 6, 4, 2, "wallet/bet_result"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 6, 4, 3, "wallet/bet_result"));
        //Validate Credit Amount wallet/bet_result "BET_WIN" with Jackpot only then "WIN" then End
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 6, 5, 1, "wallet/balance"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 6, 5, 2, "wallet/bet_result"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 6, 5, 3, "wallet/bet_result"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 6, 5, 4, "wallet/bet_result"));
        //Validate Credit Amount  wallet/bet_result "WIN" with Jackpot
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 6, 6, 1, "wallet/balance"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 6, 6, 2, "wallet/bet"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 6, 6, 3, "wallet/bet_result"));
        //Validate Credit Amount  wallet/bet_result "BET_WIN" with Jackpot
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 6, 7, 1, "wallet/balance"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 6, 7, 2, "wallet/bet_result"));
        //endregion

        //region Validate Credit Win Amount
        //Validate Rollback Debit Amount wallet/bet
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 7, 1, 1, "wallet/balance"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 7, 1, 2, "wallet/bet"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 7, 1, 3, "wallet/rollback"));
        //Validate Rollback Debit Amount wallet/bet then "END"
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 7, 2, 1, "wallet/balance"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 7, 2, 2, "wallet/bet"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 7, 2, 3, "wallet/bet_result"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 7, 2, 4, "wallet/rollback"));
        //Validate Rollback Debit Amount wallet/bet_result "BET_LOSE"
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 7, 3, 1, "wallet/balance"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 7, 3, 2, "wallet/bet_result"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 7, 3, 3, "wallet/rollback"));
        //endregion

        //region Validate idempotent
        //Validate Idempotent Debit Amount wallet/bet
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 8, 1, 1, "wallet/balance"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 8, 1, 2, "wallet/bet"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 8, 1, 3, "wallet/bet"));
        //Validate Credit Amount wallet/bet_result "WIN"
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 8, 2, 1, "wallet/balance"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 8, 2, 2, "wallet/bet"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 8, 2, 3, "wallet/bet_result"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 8, 2, 4, "wallet/bet_result"));
        //Validate Idempotent wallet/bet_result "BET_LOSE"
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 8, 3, 1, "wallet/balance"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 8, 3, 2, "wallet/bet_result"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 8, 3, 3, "wallet/bet_result"));
        //Validate Insufficient Balance wallet/bet_result "BET_WIN"
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 8, 4, 1, "wallet/balance"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 8, 4, 2, "wallet/bet_result"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 8, 4, 3, "wallet/bet_result"));
        //Validate Rollback Debit Amount wallet/bet
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 8, 5, 1, "wallet/balance"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 8, 5, 2, "wallet/bet"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 8, 5, 3, "wallet/rollback"));
        stepCases.add(new AgentIntegrationSubCaseStep(agentId, "","", 8, 5, 4, "wallet/rollback"));
        //endregion

        agentIntegrationSubCaseStepRepository.insertOrUpdate(stepCases);

    }

    public AgentIntegrationSubCaseStep getStepRecord(Integer agentId, AgentTestCaseStepUpdateDto dto) throws RecordNotFoundException {
        Optional<AgentIntegrationSubCaseStep> optional =
                Optional.ofNullable(agentIntegrationSubCaseStepRepository.findByAgentIdAndMasterCaseIdAndSubCaseIdAndStepId(agentId, dto.getMasterCaseId(), dto.getSubCaseId(), dto.getStepId()));
        optional.orElseThrow(RecordNotFoundException::new);

        return optional.get();
    }

    public void updateTestCaseStep(AgentTestCaseStepUpdateDto dto, AgentIntegrationSubCaseStep agentIntegrationSubCaseStep){
        String ip = "Unknown";
        try {
            // This exception should not block the saving of new records
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (
                UnknownHostException unknownHostException) {
            unknownHostException.printStackTrace();
        }

        agentIntegrationSubCaseStep.setUpdateById(0);
        agentIntegrationSubCaseStep.setUpdateByIp(ip);
        agentIntegrationSubCaseStep.setUpdateDate(System.currentTimeMillis());
        agentIntegrationSubCaseStep.setUpdateByUsertype(USERTYPE);
        agentIntegrationSubCaseStep.setStartTime(dto.getStartTime());
        agentIntegrationSubCaseStep.setEndTime(dto.getEndTime());
        agentIntegrationSubCaseStep.setApiUrl(dto.getApiUrl());
        agentIntegrationSubCaseStep.setRequestHeaders(dto.getRequestHeaders());
        agentIntegrationSubCaseStep.setRequestBody(dto.getRequestBody());
        agentIntegrationSubCaseStep.setResponseHttpCode(dto.getResponseHttpCode());
        agentIntegrationSubCaseStep.setResponseBody(dto.getResponseBody());
        agentIntegrationSubCaseStep.setExpectedResponse(dto.getExpectedResponse());
        agentIntegrationSubCaseStep.setStatus(dto.getStatus());
        agentIntegrationSubCaseStep.setMessageCode(dto.getMessageCode());
        agentIntegrationSubCaseStep.setRemark(dto.getRemark());

        agentIntegrationSubCaseStepRepository.save(agentIntegrationSubCaseStep);

    }
}
