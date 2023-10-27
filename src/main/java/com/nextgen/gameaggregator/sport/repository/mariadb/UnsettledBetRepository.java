package com.nextgen.gameaggregator.sport.repository.mariadb;

import com.nextgen.gameaggregator.sport.entity.UnsettledBet;
import org.springframework.data.repository.CrudRepository;

public interface UnsettledBetRepository extends CrudRepository<UnsettledBet, String> {
}
