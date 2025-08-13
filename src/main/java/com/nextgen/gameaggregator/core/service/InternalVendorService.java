package com.nextgen.gameaggregator.core.service;

import com.nextgen.gameaggregator.service.BaseVendorService;
import org.springframework.context.ApplicationContext;

public class InternalVendorService extends BaseVendorService {
    /**
     * Temporary factory method for InternalVendorService during BaseVendorService deprecation.
     * TODO: Remove this class once BaseVendorService is fully deprecated.
     */
    public static InternalVendorService getInstance(ApplicationContext applicationContext) {
        InternalVendorService vendorService = new InternalVendorService();
        // due to BaseVendorService field level autowired, manual autowire dependencies are required.
        applicationContext.getAutowireCapableBeanFactory().autowireBean(vendorService);
        return vendorService;
    }
}
