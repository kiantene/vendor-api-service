package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.*;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
import com.nextgen.gameaggregator.exception.DisabledVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.repository.ga.writer.AgentVendorLineRepository;
import com.nextgen.gameaggregator.repository.ga.writer.VendorLineCredentialRepository;
import com.nextgen.gameaggregator.repository.ga.writer.VendorLineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VendorLineService {

    private final VendorLineRepository vendorLineRepository;
    private final VendorLineCredentialRepository vendorLineCredentialRepository;
    private final AgentVendorLineRepository agentVendorLineRepository;

    @Autowired
    public VendorLineService(VendorLineRepository vendorLineRepository,
                             VendorLineCredentialRepository vendorLineCredentialRepository,
                             AgentVendorLineRepository agentVendorLineRepository) {

        this.vendorLineRepository = vendorLineRepository;
        this.vendorLineCredentialRepository = vendorLineCredentialRepository;
        this.agentVendorLineRepository = agentVendorLineRepository;
    }

    @Cacheable(value = "VendorLines", key = "{#agent.id, #vendor.id, #currency.id, #gameCategory.id}", cacheManager = "cacheManager")
    public VendorLine findAgentVendorLine(Agent agent, Vendor vendor, Currency currency, GameCategory gameCategory)
            throws InvalidVendorLineException, DisabledVendorLineException {

        List<AgentVendorLine> agentVendorLines = agentVendorLineRepository.
                findByAgentIdAndVendorIdAndCurrencyIdAndGameCategoryId(
                        agent.getId(), vendor.getId(), currency.getId(), gameCategory.getId());
        //vendor line not found
        if (agentVendorLines.isEmpty()) {
            throw new InvalidVendorLineException();
        }
        AgentVendorLine activeAgentVendorLine = null;

        for (AgentVendorLine agentVendorLine : agentVendorLines) {
            if (agentVendorLine.getStatus().equals(Status.ACTIVE.code)) {
                activeAgentVendorLine = agentVendorLine;
                break;
            }
        }

        //not active vendor line found
        if (activeAgentVendorLine == null) throw new DisabledVendorLineException();

        Integer vendorLineId = activeAgentVendorLine.getVendorLineId();
        return this.getVendorLineById(vendorLineId);
    }


    public List<AgentVendorLine> getVendorLineByAgent(Agent agent, Vendor vendor, List<Integer> currencyIds) throws InvalidVendorLineException, DisabledVendorLineException {

        List<AgentVendorLine> agentVendorLines = agentVendorLineRepository.
                findByAgentIdAndVendorIdAndCurrencyIdIn(agent.getId(), vendor.getId(), currencyIds);

        //vendor line not found
        if (agentVendorLines.isEmpty()) {
            throw new InvalidVendorLineException();
        }

        List<AgentVendorLine> activeAgentVendorLines = new ArrayList<>();

        for (AgentVendorLine agentVendorLine : agentVendorLines) {
            if (agentVendorLine.getStatus().equals(Status.ACTIVE.code)) {
                activeAgentVendorLines.add(agentVendorLine);
            }
        }

        if (activeAgentVendorLines.isEmpty()) {
            throw new DisabledVendorLineException();
        }

        return activeAgentVendorLines;
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

    @Cacheable(value = "VendorLines", key = "{#name, #value}", cacheManager = "cacheManager")
    public Integer getVendorLineIdListByNameAndValue(String name, String value) throws CredentialNotFoundException {
        final Integer ACTIVE = Status.ACTIVE.code;
        List<Integer> vendorLineIdList = vendorLineCredentialRepository.findVendorLineIdByNameAndValueAndStatus(name, value, ACTIVE);

        if (vendorLineIdList.isEmpty()) {
            throw new CredentialNotFoundException();
        }

        return vendorLineIdList.get(0);
    }

    @Cacheable(value = "VendorLines", key = "#vendorLineId", cacheManager = "cacheManager", unless = "#result == null")
    public VendorLine verifyVendorLineStatus(Integer vendorLineId) throws DisabledVendorLineException {
        VendorLine vendorLine = vendorLineRepository.findByIdAndStatus(vendorLineId, Status.ACTIVE.code);

        return Optional.ofNullable(vendorLine).orElseThrow(DisabledVendorLineException::new);
    }

    public Map<String, String> toCredentialMap(VendorLine vendorLine) {
        return vendorLine.getCredentials().stream()
                .filter(v -> v.getStatus().equals(Status.ACTIVE.code))
                .collect(Collectors.toMap(VendorLineCredential::getName, VendorLineCredential::getValue));
    }

    @Cacheable(value = "VendorLines", key = "#vendorLineId", cacheManager = "cacheManager")
    public VendorLine getVendorLineById(Integer vendorLineId) throws InvalidVendorLineException, DisabledVendorLineException {
        VendorLine vendorLine = vendorLineRepository.findById(vendorLineId)
                .orElseThrow(InvalidVendorLineException::new);

        if (vendorLine.getStatus().equals(Status.INACTIVE.code)) {
            throw new DisabledVendorLineException();
        }
        return vendorLine;
    }
}
