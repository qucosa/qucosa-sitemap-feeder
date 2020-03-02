package de.qucosa.camel.routebuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.qucosa.camel.beans.FedoraEventCreator;
import de.qucosa.camel.model.Tenant;
import de.qucosa.camel.policies.FedoraServicePolicy;
import de.qucosa.camel.policies.SitemapServicePolicy;
import de.qucosa.camel.strategies.AppendFedoraObjectInfo;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpMethods;
import org.apache.camel.component.kafka.KafkaComponent;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.component.kafka.KafkaManualCommit;

import java.util.List;

import static de.qucosa.camel.config.EndpointUris.DIRECT_CREATE_URI;
import static de.qucosa.camel.config.EndpointUris.DIRECT_DELETE_URI;
import static de.qucosa.camel.config.EndpointUris.FEDORA_3_OBJECTINFO;
import static de.qucosa.camel.config.EndpointUris.FEDORA_SERVICE_OBSERVER_URI;
import static de.qucosa.camel.config.EndpointUris.FEDORA_SERVICE_URI;
import static de.qucosa.camel.config.EndpointUris.KAFKA_BULK_DELETE_CONSUMER;
import static de.qucosa.camel.config.EndpointUris.KAFKA_BULK_INSERT_CONSUMER;
import static de.qucosa.camel.config.EndpointUris.KAFKA_SITEMAP_CONSUMER;
import static de.qucosa.camel.config.EndpointUris.PUSH_TO_SERVICE;
import static de.qucosa.camel.config.EndpointUris.SITEMAP_SERVICE_OBSERVER_URI;
import static de.qucosa.camel.config.EndpointUris.SITEMAP_SERVICE_URI;
import static de.qucosa.camel.config.RouteIds.BULK_DELETE_APPEND_OBJ_INFO;
import static de.qucosa.camel.config.RouteIds.BULK_DELETE_PUSH_TO_SERVICE;
import static de.qucosa.camel.config.RouteIds.BULK_INSERT_APPEND_OBJ_INFO;
import static de.qucosa.camel.config.RouteIds.BULK_INSERT_PUSH_TO_SERVICE;
import static de.qucosa.camel.config.RouteIds.FEDORA_3_OBJECTINFO_ID;
import static de.qucosa.camel.config.RouteIds.FEDORA_SERVICE_OBSERVER_ID;
import static de.qucosa.camel.config.RouteIds.KAFKA_BULK_DELETE_ID;
import static de.qucosa.camel.config.RouteIds.KAFKA_BULK_INSERT_ID;
import static de.qucosa.camel.config.RouteIds.KAFKA_SITEMAP_CONSUMER_ID;
import static de.qucosa.camel.config.RouteIds.SITEMAP_CONSUMER_APPEND_OBJ_INFO;
import static de.qucosa.camel.config.RouteIds.SITEMAP_CONSUMER_PUSH_TO_SERVICE;
import static de.qucosa.camel.config.RouteIds.SITEMAP_CREATE_URL_ID;
import static de.qucosa.camel.config.RouteIds.SITEMAP_DELETE_URL_ID;
import static de.qucosa.camel.config.RouteIds.SITEMAP_SERVICE_OBSERVER_ID;

public class SitemapFeederRoutes extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        KafkaComponent kafka = new KafkaComponent();
        kafka.setBrokers("{{kafka.broker.host}}");
        getContext().addComponent("kafka", kafka);

        from(FEDORA_SERVICE_OBSERVER_URI)
                .routeId(FEDORA_SERVICE_OBSERVER_ID)
                .autoStartup(false)
                .to(FEDORA_SERVICE_URI);

        from(SITEMAP_SERVICE_OBSERVER_URI)
                .routeId(SITEMAP_SERVICE_OBSERVER_ID)
                .autoStartup(false)
                .to(SITEMAP_SERVICE_URI);

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
                        .endChoice()
                    .otherwise()
                        .to(DIRECT_DELETE_URI)
                .endChoice();

        // This route is for fedora 3 only.
        from(FEDORA_3_OBJECTINFO)
                .routeId(FEDORA_3_OBJECTINFO_ID)
                .routePolicy(new FedoraServicePolicy())
                .setProperty("pid", jsonpath("$['org.fcrepo.jms.identifier']"))
                .recipientList(simple(FEDORA_SERVICE_URI + "fedora/objects/${property.pid}?format=xml"));

        from(DIRECT_CREATE_URI)
                .routeId(SITEMAP_CREATE_URL_ID)
                .routePolicy(new SitemapServicePolicy())
                .log("create_url (json): ${body}")
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.POST))
                .setHeader(Exchange.CHARSET_NAME, constant("UTF-8"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .throttle(10)
                .to(SITEMAP_SERVICE_URI)
                .process(exchange -> {

                    if (exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class) == 201) {
                        KafkaManualCommit commit = exchange.getIn().getHeader(KafkaConstants.MANUAL_COMMIT, KafkaManualCommit.class);
                        commit.commitSync();
                    }
                });

        from(DIRECT_DELETE_URI)
                .routeId(SITEMAP_DELETE_URL_ID)
                .routePolicy(new SitemapServicePolicy())
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.DELETE))
                .setHeader(Exchange.CHARSET_NAME, constant("UTF-8"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .to(SITEMAP_SERVICE_URI);

        from(KAFKA_BULK_INSERT_CONSUMER)
                .routeId(KAFKA_BULK_INSERT_ID)
                .setProperty("eventType", simple("create"))
                .bean(FedoraEventCreator.class, "createEvent")
                .enrich(FEDORA_3_OBJECTINFO, new AppendFedoraObjectInfo(tenants())).id(BULK_INSERT_APPEND_OBJ_INFO)
                .to(PUSH_TO_SERVICE).id(BULK_INSERT_PUSH_TO_SERVICE)
                .process(exchange -> {

                    if (exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class) == 201) {
                        KafkaManualCommit commit = exchange.getIn().getHeader(KafkaConstants.MANUAL_COMMIT, KafkaManualCommit.class);
                        commit.commitSync();
                    }
                });

        from(KAFKA_BULK_DELETE_CONSUMER)
                .routeId(KAFKA_BULK_DELETE_ID)
                .setProperty("eventType", simple("delete"))
                .bean(FedoraEventCreator.class, "createEvent")
                .enrich(FEDORA_3_OBJECTINFO, new AppendFedoraObjectInfo(tenants())).id(BULK_DELETE_APPEND_OBJ_INFO)
                .to(PUSH_TO_SERVICE).id(BULK_DELETE_PUSH_TO_SERVICE)
                .process(exchange -> {

                    if (exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class) == 204) {
                        KafkaManualCommit commit = exchange.getIn().getHeader(KafkaConstants.MANUAL_COMMIT, KafkaManualCommit.class);
                        commit.commitSync();
                    }
                });
    }

    private List<Tenant> tenants() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(getClass().getResourceAsStream(getContext().resolvePropertyPlaceholders("{{conf.path}}") + "tenant.json"),
                objectMapper.getTypeFactory().constructCollectionType(List.class, Tenant.class));
    }
}
