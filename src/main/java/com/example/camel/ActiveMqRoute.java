package com.example.camel;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpMethods;
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
                .convertBodyTo(String.class, "UTF-8")
                .setHeader(KafkaConstants.KEY, exchangeProperty("pid"))
                .to("kafka:mets_updates");

        // Send XML to ExistDB
        from("kafka:mets_updates?groupId=existdb_feeder")
                .id("existdb_feeder")
                .noAutoStartup()
                .setProperty("pid", header(KafkaConstants.KEY))
                .process(exchange -> {
                    String pid = exchange.getIn().getHeader(KafkaConstants.KEY, String.class);
                    exchange.getIn().setHeader(Exchange.HTTP_PATH,
                            String.format("/db/qucosa/mets/test/%s.mets.xml", pid.replaceFirst(":", "-")));
                    exchange.setProperty("pid", pid);
                })
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.PUT))
                .setHeader(Exchange.CHARSET_NAME, constant("UTF-8"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/xml"))
                .throttle(10)
                .to("http4://sdvexistdb01:8080/exist/rest" +
                        "?authUsername=qucosa-ingest" +
                        "&authPassword=qucosa-ingest")
                .log("Updated ${header.CamelHttpPath}");
    }
}
