package com.github.h3nrique.postalcode.service;

import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PostalCodeServiceTest {

    private static final Logger log = LoggerFactory.getLogger(PostalCodeServiceTest.class);
    private PostalCodeService postalCodeService;

    @Before
    public void setUp() {
        this.postalCodeService = new PostalCodeService();
    }

    @Test
    public void findCepOk() {
        Map<String, String> cepResult = postalCodeService.find("03694090");
        assertFalse(cepResult.isEmpty());
        log.debug("cepResult :: [{}]", cepResult);
    }

    @Test
    public void findCepNok() {
        Map<String, String> cepResult = postalCodeService.find("000000000");
        assertTrue(cepResult.isEmpty());
    }
}
