package de.qucosa.beans;

import de.qucosa.camel.beans.FedoraEventCreator;
import de.qucosa.events.FedoraUpdateEvent;
import org.apache.camel.Exchange;
import org.apache.camel.builder.ExchangeBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(value = MethodOrderer.OrderAnnotation.class)
@DisplayName("Test the FedoraEventCreator if come pid's from bulk kafka topics.")
public class FedoraEventCreatorTest {
    private final DefaultCamelContext camelContext = new DefaultCamelContext();

    private Exchange exchange;

    private final FedoraEventCreator eventCreator = new FedoraEventCreator();

    @BeforeEach
    public void readPid() {
        exchange = ExchangeBuilder.anExchange(camelContext).withBody("qucosa:12164").build();
    }

    @AfterEach
    public void shotdownAfterTest() {
        exchange.getOut().getBody();
        exchange.removeProperty("eventType");
    }

    @Test
    @DisplayName("Test if the eventType is create.")
    public void create() throws Exception {
        exchange.setProperty("eventType", "create");
        eventCreator.createEvent(exchange);
        FedoraUpdateEvent event = exchange.getIn().getBody(FedoraUpdateEvent.class);

        assertEquals("qucosa:12164", event.getIdentifier());
        assertEquals("create", event.getEventType());
    }

    @Test
    @DisplayName("Test if the eventType is delete.")
    public void delete() throws Exception {
        exchange.setProperty("eventType", "delete");
        eventCreator.createEvent(exchange);
        FedoraUpdateEvent event = exchange.getIn().getBody(FedoraUpdateEvent.class);

        assertEquals("qucosa:12164", event.getIdentifier());
        assertEquals("delete", event.getEventType());
    }
}
