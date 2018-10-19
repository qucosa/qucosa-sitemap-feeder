package com.example.camel;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaComponent;
import org.apache.camel.component.kafka.KafkaConstants;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class ActiveMqRoute extends RouteBuilder {

    @Override
    public void configure() {
        // setup kafka component with the brokers
        KafkaComponent kafka = new KafkaComponent();
        kafka.setBrokers("localhost:9092");
        getContext().addComponent("kafka", kafka);

        // Transport updated PIDs to Kafka topic
        from("activemq:topic:fedora.apim.updates")
                .id("fcrepo_updates")
                .noAutoStartup()
                .transform(xpath("/atom:entry/atom:summary[@type='text']/text()", String.class)
                        .namespace("atom", "http://www.w3.org/2005/Atom"))
                .to("kafka:fcrepo_updates");

        // Obtain and post METS XML to Kafka topic
        from("kafka:fcrepo_updates?groupId=mets_dissemination")
                .id("mets_update")
                .resequence().body().timeout(TimeUnit.SECONDS.toMillis(5))
                .setProperty("pid", body())
                .setHeader(Exchange.HTTP_QUERY, simple("pid=${exchangeProperty[pid]}"))
                .throttle(1)
                .to("http4://sdvcmr-app03:8080/mets")
                .setHeader(KafkaConstants.KEY, exchangeProperty("pid"))
                .to("kafka:mets_updates");

    }
}
