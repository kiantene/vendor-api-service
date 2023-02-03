package com.nextgen.gameaggregator.repository;

import com.nextgen.gameaggregator.entity.Languages;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LanguageRepository extends JpaRepository<Languages, Integer> {
    Languages findByCode(String code);
}
