package com.example.camel;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class ActiveMqRoute extends RouteBuilder {

    @Override
    public void configure() {

        from("activemq:topic:fedora.apim.*")
                .id("ActiveMQ-updates-route")
                .log("${body}");
    }
}
