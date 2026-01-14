package com.nextgen.gameaggregator.vendor.lucky365.api.rollback;

import com.nextgen.core.exception.InternalServerException;
import org.springframework.stereotype.Service;

@Service
public class MultiRollbackService {
    public RollbackResponse process(RollbackRequest rollbackRequest) {
        // we cannot handle multiple rollback list
        throw new InternalServerException("Multiple rollback are not allowed");
    }
}
