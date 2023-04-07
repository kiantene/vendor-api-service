package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.*;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.*;
import com.nextgen.gameaggregator.repository.AgentVendorLineRepository;
import com.nextgen.gameaggregator.repository.VendorLineCredentialRepository;

import com.nextgen.gameaggregator.repository.VendorLineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VendorLineService {
    @Autowired
    private VendorLineRepository vendorLineRepository;
    @Autowired
    private VendorLineCredentialRepository vendorLineCredentialRepository;
    @Autowired
    private AgentVendorLineRepository agentVendorLineRepository;


    public VendorLine findAgentVendorLine(Agent agent, Vendor vendor,Currency currency , GameCategory gameCategory)
            throws InvalidVendorLineException, DisabledVendorLineException {

        List<AgentVendorLine>  agentVendorLines = agentVendorLineRepository.
                findByAgentIdAndVendorIdAndCurrencyIdAndGameCategoryId(
                        agent.getId(), vendor.getId(), currency.getId(), gameCategory.getId());
        //vendor line not found
        if (agentVendorLines.isEmpty()) {
            throw new InvalidVendorLineException();
        }
        AgentVendorLine activeAgentVendorLine = null;

        for (AgentVendorLine agentVendorLine : agentVendorLines) {
            if(agentVendorLine.getStatus().equals(Status.ACTIVE.code)){
                activeAgentVendorLine = agentVendorLine;
                break;
            }
        }
        //not active vendor line found
        Optional.ofNullable(activeAgentVendorLine).orElseThrow(DisabledVendorLineException::new);

        return activeAgentVendorLine.getVendorLine();
    }



    public List<AgentVendorLine> getVendorLineByAgent(Integer agentId, Integer vendorId, Integer currencyId) throws NoAvailableLineException, InvalidVendorLineException {
        final Integer ACTIVE = Status.ACTIVE.code;

        List<AgentVendorLine> agentVendorLines = agentVendorLineRepository.findByAgentIdAndVendorIdAndCurrencyIdAndStatus(agentId, vendorId, currencyId, ACTIVE);
        Optional.ofNullable(agentVendorLines).orElseThrow(InvalidVendorLineException::new);

        //TODO (bu Alex), to discuss should validated agent supported vendor
//        if(agentVendorLines.isEmpty()){
//            throw new NoAvailableLineException();
//        }

        return agentVendorLines;
    }

    @Cacheable(value = "VendorLineCredentials", key = "{#vendorLineId, #name}", cacheManager = "cacheManager")
    public String getCredentialValueByName(Integer vendorLineId, String name) throws CredentialNotFoundException {
        final Integer ACTIVE = Status.ACTIVE.code;
        VendorLineCredential credential = vendorLineCredentialRepository.findByVendorLineIdAndNameAndStatus(vendorLineId, name, ACTIVE);
        Optional.ofNullable(credential).orElseThrow(CredentialNotFoundException::new);

        if (credential.getValue().isEmpty()) {
            throw new CredentialNotFoundException();
        }
        return credential.getValue();
    }

    @Cacheable(value = "VendorLineCredentials", key = "{#name, #value}", cacheManager = "cacheManager")
    public Integer getVendorLineIdByNameAndValue(String name, String value) throws CredentialNotFoundException {
        final Integer ACTIVE = Status.ACTIVE.code;
        VendorLineCredential credential = vendorLineCredentialRepository.findByNameAndValueAndStatus(name, value, ACTIVE);
        Optional.ofNullable(credential).orElseThrow(CredentialNotFoundException::new);

        if (credential.getValue().isEmpty()) {
            throw new CredentialNotFoundException();
        }
        return credential.getVendorLineId();
    }

    @Cacheable(value = "VendorLines", key = "#vendorLineId", cacheManager = "cacheManager")
    public Integer verifyVendorLineStatus(Integer vendorLineId) throws DisabledVendorLineException {
        VendorLine vendorLine = vendorLineRepository.findByIdAndStatus(vendorLineId, Status.ACTIVE.code);
        Optional.ofNullable(vendorLine).orElseThrow(DisabledVendorLineException::new);

        return vendorLine.getId();
    }

    public Map<String, String> toCredentialMap(VendorLine vendorLine) {
        return vendorLine.getCredentials().stream()
                .filter(v -> v.getStatus().equals(Status.ACTIVE.code))
                .collect(Collectors.toMap(VendorLineCredential::getName, VendorLineCredential::getValue));
    }

    public VendorLine getVendorLineById(Integer vendorLineId) throws InvalidVendorLineException {
        VendorLine vendorLine = vendorLineRepository.findByIdAndStatus(vendorLineId, Status.ACTIVE.code);
        Optional.ofNullable(vendorLine).orElseThrow(InvalidVendorLineException::new);

        return vendorLine;
    }
}
