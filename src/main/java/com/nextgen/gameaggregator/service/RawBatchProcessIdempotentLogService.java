package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.RawBatchProcessIdempotentLog;
import com.nextgen.gameaggregator.repository.RawBatchProcessIdempotentLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RawBatchProcessIdempotentLogService {

    @Autowired
    private RawBatchProcessIdempotentLogRepository rawBatchProcessIdempotentLogRepository;

    public void create(RawBatchProcessIdempotentLog rawBatchProcessIdempotentLog) {
        rawBatchProcessIdempotentLogRepository.save(rawBatchProcessIdempotentLog);
    }

    public RawBatchProcessIdempotentLog checkExists(String id) {

        return rawBatchProcessIdempotentLogRepository.findById(id).orElse(null);
    }
}
