package com.example.camel;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class HelloRoute extends RouteBuilder {

    private String name;

    @Override
    public void configure() {
        rest("/say")
                .produces("text/plain")
                .consumes("text/plain");

        rest("/say").get("/hello").to("direct:hello");
        rest("/say").post("/hello").to("direct:update");

        from("direct:hello")
                .process(exchange -> exchange.getIn().setHeader("name", name))
                .transform().simple("Hello ${header.name}");

        from("direct:update")
                .log("${body}")
                .process(exchange -> name = exchange.getIn().getBody(String.class))
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(HttpStatus.ACCEPTED.value()));
    }
}
