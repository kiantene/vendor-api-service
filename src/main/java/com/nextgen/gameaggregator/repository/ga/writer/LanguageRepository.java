package com.nextgen.gameaggregator.repository.ga.writer;

import com.nextgen.gameaggregator.entity.ga.Language;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LanguageRepository extends JpaRepository<Language, Integer> {
    Language findByCode(String code);

    @Override
    List<Language> findAll();
}
