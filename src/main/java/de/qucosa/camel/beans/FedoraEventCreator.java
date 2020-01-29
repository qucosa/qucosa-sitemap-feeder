package de.qucosa.camel.beans;

import de.qucosa.events.FedoraUpdateEvent;
import org.apache.camel.Exchange;

public class FedoraEventCreator {

    public void createEvent(Exchange exchange) {
        FedoraUpdateEvent event = new FedoraUpdateEvent();
        event.setEventType(exchange.getProperty("eventType").toString());
        event.setIdentifier(exchange.getIn().getBody(String.class));
        exchange.getIn().setBody(event);
    }
}
