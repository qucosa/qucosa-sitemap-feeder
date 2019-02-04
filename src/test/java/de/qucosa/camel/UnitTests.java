package de.qucosa.camel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.camel.Processor;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.json.simple.JsonObject;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnitTests extends BaseEmbeddedKafkaTest {

    private final Logger log = LoggerFactory.getLogger(UnitTests.class);

//    @EndpointInject(uri = "kafka:" + TOPIC
//        + "?groupId=group1&autoOffsetReset=earliest&keyDeserializer=org.apache.kafka.common.serialization.StringDeserializer&"
//        + "valueDeserializer=org.apache.kafka.common.serialization.StringDeserializer"
//        + "&autoCommitIntervalMs=1000&sessionTimeoutMs=30000&autoCommitEnable=true&interceptorClasses=org.apache.camel.component.kafka.MockConsumerInterceptor")

//    @EndpointInject(uri = "kafka:sitemap_feeder?groupId=modifysitemap")
//    private Endpoint to;
//    @EndpointInject(uri = "mock:message")
//    private MockEndpoint from;

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new ActiveMqRoute();
    }

//    @Override
//    protected AbstractApplicationContext createApplicationContext() {
//        return null;
//    }

//    @Override
//    protected CamelContext createCamelContext() throws Exception {
//        CamelContext context = super.createCamelContext();
//
//        PropertiesComponent pc = new PropertiesComponent();
//        pc.setLocation("classpath:test-application.properties");
//        context.addComponent("properties", pc);

//        return context;
//    }

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @Before
    public void modifyAmqRoute() throws Exception {
        AdviceWithRouteBuilder amqUpdateMessageRoute = new AdviceWithRouteBuilder() {
            @Override
            public void configure() {
                // replace Fedora-AMQ-Endpoint with mocked one from
                replaceFromWith("file://src/test/resources/jms/?fileName=ingest.xml&noop=true");
            }
        };

        AdviceWithRouteBuilder appendFedoraObjectInfo = new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById("appendFedoraObjectInfo").replace().process((Processor) exchange -> {
                    String jsonBodyAsString = exchange.getIn().getBody(String.class);
                    ObjectMapper mapper = new ObjectMapper();

                    JsonNode jsonBody = mapper.readTree(jsonBodyAsString);
                    ObjectNode node = (ObjectNode) jsonBody;

                    node.put("tenant_urlset", "testmandant");
                    node.put("tenant_url", "testmandant.qucosa.de");
                    node.put("objectState", "I");

                    exchange.getIn().setBody(node.toString(), JsonObject.class);
                })
                .log("JSONMESSAGE: ${body}");
            }
        };

        AdviceWithRouteBuilder interceptHttpEndpoints = new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint("direct:*")
                        .log("Body: ${body}");
            }
        };

        context.getRouteDefinition("amqMessage").adviceWith(context, amqUpdateMessageRoute);
        context.getRouteDefinition("sitemap_feeder").adviceWith(context, appendFedoraObjectInfo);
        context.getRouteDefinition("sitemap_feeder").adviceWith(context, interceptHttpEndpoints);
    }

    @Test
    public void amq_document_is_inactive_filter() throws Exception {
        context.start();
        Thread.sleep(5000);
        assertTrue(true);
        context.stop();
    }

    /* TODO unit tests ausbauen */
}
