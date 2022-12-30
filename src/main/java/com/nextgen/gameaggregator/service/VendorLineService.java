package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.AgentVendorLine;
import com.nextgen.gameaggregator.entity.VendorLine;
import com.nextgen.gameaggregator.entity.VendorLineCredential;
import com.nextgen.gameaggregator.exception.CredentialNotFoundException;
import com.nextgen.gameaggregator.exception.NoAvailableLineException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.repository.AgentVendorLineRepository;
import com.nextgen.gameaggregator.repository.VendorLineCredentialRepository;
import com.nextgen.gameaggregator.repository.VendorLineRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

//    public VendorLine getVendorLine(Integer vendorLineId) throws InvalidVendorLineException {
//        Optional<VendorLine> entity = vendorLineRepository.findById(vendorLineId);
//        entity.orElseThrow(InvalidVendorLineException::new);
//        return entity.get();
//    }

    public VendorLine getVendorLineByAgent(Integer agentId, Integer vendorId, Integer currencyId) throws NoAvailableLineException, InvalidVendorLineException {
        AgentVendorLine agentVendorLine = agentVendorLineRepository.findByAgentIdAndVendorIdAndCurrencyId(agentId, vendorId, currencyId);
        Optional.ofNullable(agentVendorLine).orElseThrow(InvalidVendorLineException::new);

        VendorLine vendorLine = agentVendorLine.getVendorLine();
        if (vendorLine == null || agentVendorLine.getStatus() == 0 || vendorLine.getStatus() == 0) {
            throw new NoAvailableLineException();
        }
        return vendorLine;
    }

    public String getCredentialValueByName(Integer vendorLineId, String name) throws CredentialNotFoundException {
        final Integer status = 1;
        VendorLineCredential credential = vendorLineCredentialRepository.findByVendorLineIdAndNameAndStatus(vendorLineId, name, status);
        Optional.ofNullable(credential).orElseThrow(CredentialNotFoundException::new);

        if (credential.getValue().isEmpty()) {
            throw new CredentialNotFoundException();
        }
        return credential.getValue();
    }

    public Map<String, String> toCredentialMap(VendorLine vendorLine) {
        return vendorLine.getCredentials().stream()
                .filter(v -> v.getStatus() == 1)
                .collect(Collectors.toMap(VendorLineCredential::getName, VendorLineCredential::getValue));
    }
}
