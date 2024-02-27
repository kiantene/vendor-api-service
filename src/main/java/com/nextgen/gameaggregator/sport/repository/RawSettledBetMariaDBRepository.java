package com.nextgen.gameaggregator.sport.repository;

import com.nextgen.gameaggregator.sport.entity.SportRawSettledBet;
import org.springframework.data.repository.CrudRepository;

public interface RawSettledBetMariaDBRepository extends CrudRepository<SportRawSettledBet, String> {
}
