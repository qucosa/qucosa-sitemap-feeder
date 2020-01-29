package de.qucosa.camel.processors;

import de.qucosa.events.FedoraUpdateEvent;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class FedoraEventCreator implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        FedoraUpdateEvent event = new FedoraUpdateEvent();
        event.setEventType("create");
        event.setIdentifier(exchange.getIn().getBody(String.class));
        exchange.getIn().setBody(event);
    }
}
