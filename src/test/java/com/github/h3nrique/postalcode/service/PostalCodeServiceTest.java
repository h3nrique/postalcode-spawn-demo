package com.github.h3nrique.postalcode.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostalCodeServiceTest {

    private static final Logger log = LoggerFactory.getLogger(PostalCodeServiceTest.class);
    private static PostalCodeService postalCodeService;

    @BeforeAll
    public static void setUp() {
        postalCodeService = new PostalCodeService();
    }

    @Test
    public void findCepOk() {
        Optional<Map<String, String>> cepResult = postalCodeService.find("03568000");
        assertTrue(cepResult.isPresent());
        log.debug("cepResult :: [{}]", cepResult.get());
    }

    @Test
    public void findCepNok() {
        Optional<Map<String, String>> cepResult = postalCodeService.find("000000000");
        assertFalse(cepResult.isPresent());
    }
}
