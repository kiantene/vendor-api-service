package com.nextgen.gameaggregator.core.retry.couchbase;

import org.springframework.beans.factory.annotation.Qualifier;
import com.couchbase.client.java.Collection;
import org.springframework.stereotype.Repository;

@Repository
public class HttpRetryJobRepository {
    private final Collection collection;

    public HttpRetryJobRepository(@Qualifier("httpRetryJobsCollection") Collection collection) {
        this.collection = collection;
    }

    public void insert(HttpRetryJob document) {
        collection.insert(document.getId(), document);
    }
}
