package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.Platform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlatformRepository extends JpaRepository<Platform, Integer> {
    Platform findByCode(String code);
}
