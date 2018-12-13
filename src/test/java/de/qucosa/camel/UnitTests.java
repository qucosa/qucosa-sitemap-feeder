package de.qucosa.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnitTests extends CamelTestSupport {

    private final Logger log = LoggerFactory.getLogger(UnitTests.class);

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new ActiveMqRoute();
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {
        CamelContext context = super.createCamelContext();

        PropertiesComponent pc = new PropertiesComponent();
        pc.setLocation("classpath:test-application.properties");
        context.addComponent("properties", pc);

        return context;
    }

    @Test
    @Ignore("test not working yet")
    public void testCreateUrlset() throws Exception {
        context.start();

        Exchange exchange = createExchangeWithBody("qucosa:70489");
        exchange.setProperty("method", "ingest");

        template.send("kafka:fcrepo_updates", exchange);

        assertTrue(true);
    }

    /* TODO unit tests ausbauen */
}
