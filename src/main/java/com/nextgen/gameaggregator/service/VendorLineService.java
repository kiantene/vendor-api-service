package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.AgentVendorLine;
import com.nextgen.gameaggregator.entity.VendorLine;
import com.nextgen.gameaggregator.entity.VendorLineCredential;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
import com.nextgen.gameaggregator.exception.DisabledVendorLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.exception.NoAvailableLineException;
import com.nextgen.gameaggregator.repository.AgentVendorLineRepository;
import com.nextgen.gameaggregator.repository.VendorLineCredentialRepository;
import com.nextgen.gameaggregator.repository.VendorLineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

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

    public VendorLine getVendorLineByAgent(Integer agentId, Integer vendorId, Integer currencyId) throws NoAvailableLineException, InvalidVendorLineException {
        AgentVendorLine agentVendorLine = agentVendorLineRepository.findByAgentIdAndVendorIdAndCurrencyId(agentId, vendorId, currencyId);
        Optional.ofNullable(agentVendorLine).orElseThrow(InvalidVendorLineException::new);

        VendorLine vendorLine = agentVendorLine.getVendorLine();
        final Integer INACTIVE = Status.INACTIVE.code;
        if (vendorLine == null || agentVendorLine.getStatus().equals(INACTIVE) || vendorLine.getStatus().equals(INACTIVE)) {
            throw new NoAvailableLineException();
        }
        return vendorLine;
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
}
