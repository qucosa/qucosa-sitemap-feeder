package com.example.camel;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnitTests {

    private Logger log = LoggerFactory.getLogger(UnitTests.class);

    @Test
    public void hello() {
        log.info("Hello!");
    }

}
