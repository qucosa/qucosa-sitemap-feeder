package com.example.camel;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnitTests extends CamelTestSupport {

    private Logger log = LoggerFactory.getLogger(UnitTests.class);

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new ActiveMqRoute();
    }

    @Test
    public void hello() {
        log.info("Hello!");
    }

    @Test
    public void testCreateUrlset() throws Exception {
        context.start();

        Exchange exchange = createExchangeWithBody("qucosa:70489");
        exchange.setProperty("method", "ingest");

        template.send("kafka:fcrepo_updates", exchange);

        assertTrue(true);
    }
}
