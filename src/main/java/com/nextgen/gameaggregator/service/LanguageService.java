package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.Language;
import com.nextgen.gameaggregator.exception.InvalidLanguageException;
import com.nextgen.gameaggregator.repository.LanguageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class LanguageService {

    @Autowired
    private LanguageRepository languageRepository;

    public Language checkLanguageCode(String languageCode) throws InvalidLanguageException {
        Language language = languageRepository.findByCode(languageCode);
        Optional.ofNullable(language).orElseThrow(InvalidLanguageException::new);
        return language;

    }
}
