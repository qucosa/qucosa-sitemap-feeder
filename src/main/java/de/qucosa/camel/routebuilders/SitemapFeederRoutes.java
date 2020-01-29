package de.qucosa.camel.routebuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.qucosa.camel.model.Tenant;
import de.qucosa.camel.strategies.AppendFedoraObjectInfo;
import de.qucosa.events.FedoraUpdateEvent;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpMethods;
import org.apache.camel.component.kafka.KafkaComponent;

import java.util.List;

import static de.qucosa.camel.config.EndpointUris.DIRECT_CREATE_URI;
import static de.qucosa.camel.config.EndpointUris.DIRECT_DELETE_URI;
import static de.qucosa.camel.config.EndpointUris.FEDORA_3_OBJECTINFO;
import static de.qucosa.camel.config.EndpointUris.KAFKA_BULK_INSERT_CONSUMER;
import static de.qucosa.camel.config.EndpointUris.KAFKA_BULK_INSERT_ROUTE;
import static de.qucosa.camel.config.EndpointUris.KAFKA_SITEMAP_CONSUMER;
import static de.qucosa.camel.config.EndpointUris.PUSH_TO_SERVICE;
import static de.qucosa.camel.config.EndpointUris.SITEMAP_SERVICE_URI;
import static de.qucosa.camel.config.RouteIds.BULK_INSERT_APPEND_OBJ_INFO;
import static de.qucosa.camel.config.RouteIds.BULK_INSERT_PUSH_TO_SERVICE;
import static de.qucosa.camel.config.RouteIds.FEDORA_3_OBJECTINFO_ID;
import static de.qucosa.camel.config.RouteIds.KAFKA_SITEMAP_CONSUMER_ID;
import static de.qucosa.camel.config.RouteIds.SITEMAP_CONSUMER_APPEND_OBJ_INFO;
import static de.qucosa.camel.config.RouteIds.SITEMAP_CONSUMER_PUSH_TO_SERVICE;
import static de.qucosa.camel.config.RouteIds.SITEMAP_CREATE_URL_ID;
import static de.qucosa.camel.config.RouteIds.SITEMAP_DELETE_URL_ID;

public class SitemapFeederRoutes extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        KafkaComponent kafka = new KafkaComponent();
        kafka.setBrokers("{{kafka.broker.host}}");
        getContext().addComponent("kafka", kafka);

        from(KAFKA_SITEMAP_CONSUMER)
                .routeId(KAFKA_SITEMAP_CONSUMER_ID)
                .enrich(FEDORA_3_OBJECTINFO, new AppendFedoraObjectInfo(tenants())).id(SITEMAP_CONSUMER_APPEND_OBJ_INFO)
                .to(PUSH_TO_SERVICE).id(SITEMAP_CONSUMER_PUSH_TO_SERVICE);

        from(PUSH_TO_SERVICE).routeId("push_service")
                .choice()
                    .when(simple("${property.objectState} == 'A'"))
                        .choice()
                            .when(simple("${property.eventType} == 'create' or ${property.eventType} == 'update'"))
                                .to(DIRECT_CREATE_URI)
                            .when(simple("${property.eventType} == 'delete'"))
                                .to(DIRECT_DELETE_URI)
                    .otherwise()
                        // @TODO delete url from sitemap
                        .to("mock:otherwise");


        // This route is for fedora 3 only.
        from(FEDORA_3_OBJECTINFO)
                .routeId(FEDORA_3_OBJECTINFO_ID)
                .setProperty("pid", jsonpath("$['org.fcrepo.jms.identifier']"))
                .recipientList(simple("http4://{{fedora.service.url}}/fedora/objects/${property.pid}?format=xml"));

        from(DIRECT_CREATE_URI)
                .routeId(SITEMAP_CREATE_URL_ID)
                .log("create_url (json): ${body}")
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.POST))
                .setHeader(Exchange.CHARSET_NAME, constant("UTF-8"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .throttle(10)
                .to(SITEMAP_SERVICE_URI);

        from(DIRECT_DELETE_URI)
                .routeId(SITEMAP_DELETE_URL_ID)
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.DELETE))
                .setHeader(Exchange.CHARSET_NAME, constant("UTF-8"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .to("http4://{{sitemap.service.url}}/url?throwExceptionOnFailure=false");

        // route to update sitemap via pid's (post qucosa-ID's (qucosa:12345) to kafka topic "pidupdate")
        from(KAFKA_BULK_INSERT_CONSUMER)
                .routeId(KAFKA_BULK_INSERT_ROUTE)
                .process(exchange -> {
                    FedoraUpdateEvent event = new FedoraUpdateEvent();
                    event.setEventType("create");
                    event.setIdentifier(exchange.getIn().getBody(String.class));
                    exchange.getIn().setBody(event);
                })
                .enrich(FEDORA_3_OBJECTINFO, new AppendFedoraObjectInfo(tenants())).id(BULK_INSERT_APPEND_OBJ_INFO)
                // set/get method/tenant/pid/encodedpid
                .to(PUSH_TO_SERVICE).id(BULK_INSERT_PUSH_TO_SERVICE);




        // route to update sitemap via pid's (post qucosa-ID's (qucosa:12345) to kafka topic "pidupdate")
//        from("kafka:piddelete?groupId=bulkdelete")
//                .routeId(KAFKA_BULK_DELETE_ROUTE)
//                // set/get method/tenant/pid/encodedpid
//                .to("kafka:sitemap_feeder");
    }

    private List<Tenant> tenants() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        List<Tenant> tenants = objectMapper.readValue(getClass().getResourceAsStream(getContext().resolvePropertyPlaceholders("{{conf.path}}") + "tenant.json"),
                objectMapper.getTypeFactory().constructCollectionType(List.class, Tenant.class));
        return tenants;
    }
}
