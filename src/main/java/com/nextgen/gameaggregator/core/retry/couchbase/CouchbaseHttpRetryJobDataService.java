package com.nextgen.gameaggregator.core.retry.couchbase;

import org.springframework.stereotype.Service;

@Service
public class CouchbaseHttpRetryJobDataService {
    private final HttpRetryJobRepository repo;

    public CouchbaseHttpRetryJobDataService(HttpRetryJobRepository repo) {
        this.repo = repo;
    }

    public void insert(HttpRetryJob job) {
        repo.insert(job);
    }
}
