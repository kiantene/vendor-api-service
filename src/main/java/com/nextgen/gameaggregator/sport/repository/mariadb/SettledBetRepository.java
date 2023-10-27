package com.nextgen.gameaggregator.sport.repository.mariadb;

import com.nextgen.gameaggregator.sport.entity.SettledBet;
import org.springframework.data.repository.CrudRepository;

public interface SettledBetRepository extends CrudRepository<SettledBet, String> {
}
