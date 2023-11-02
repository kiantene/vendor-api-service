package com.nextgen.gameaggregator.sport.repository;

import com.nextgen.gameaggregator.sport.entity.SportSettledBet;
import org.springframework.data.repository.CrudRepository;

public interface SettledBetMariaDBRepository extends CrudRepository<SportSettledBet, String> {
}
