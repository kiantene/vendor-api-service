package com.nextgen.gameaggregator.vendor.cq9.service;

import com.nextgen.gameaggregator.entity.ga.Vendor;
import com.nextgen.gameaggregator.exception.InvalidVendorException;
import com.nextgen.gameaggregator.repository.ga.writer.VendorRepository;
import com.nextgen.gameaggregator.service.BaseVendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class VendorService extends BaseVendorService {

    @Autowired
    private VendorRepository vendorRepository;

    public Vendor findVendorByCode(String vendorCode) throws InvalidVendorException {
        Vendor vendor = vendorRepository.findByCode(vendorCode);
        Optional.ofNullable(vendor).orElseThrow(InvalidVendorException::new);
        return vendor;
    }
}
