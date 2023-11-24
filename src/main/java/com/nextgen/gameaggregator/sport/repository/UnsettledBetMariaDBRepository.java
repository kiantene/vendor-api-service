package com.nextgen.gameaggregator.sport.repository;

import com.nextgen.gameaggregator.sport.entity.SportUnsettledBetCouchbase;
import org.springframework.data.repository.CrudRepository;

public interface UnsettledBetMariaDBRepository extends CrudRepository<SportUnsettledBetCouchbase, String> {
}
