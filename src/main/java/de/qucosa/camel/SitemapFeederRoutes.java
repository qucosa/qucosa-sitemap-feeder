package de.qucosa.camel;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.qucosa.camel.model.Tenant;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpMethods;
import org.apache.camel.component.kafka.KafkaComponent;

import java.util.List;

import static de.qucosa.camel.config.EndpointUris.DIRECT_CREATE_URL;
import static de.qucosa.camel.config.EndpointUris.FEDORA_3_OBJECTINFO;
import static de.qucosa.camel.config.EndpointUris.KAFKA_SITEMAP_CONSUMER;
import static de.qucosa.camel.config.EndpointUris.SITEMAP_SERVICE_CREATE_URL;
import static de.qucosa.camel.config.RouteIds.APPEND_FEDORA_3_OBJ_INFO;
import static de.qucosa.camel.config.RouteIds.FEDORA_3_OBJECTINFO_ID;
import static de.qucosa.camel.config.RouteIds.KAFKA_SITEMAP_CONSUMER_ID;
import static de.qucosa.camel.config.RouteIds.SITEMAP_CREATE_URL_ID;

public class SitemapFeederRoutes extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        KafkaComponent kafka = new KafkaComponent();
        kafka.setBrokers("{{kafka.broker.host}}");
        getContext().addComponent("kafka", kafka);

        from(KAFKA_SITEMAP_CONSUMER)
                .routeId(KAFKA_SITEMAP_CONSUMER_ID)
                .enrich("seda:objectinfo", new AppendFedoraObjectInfo(tenants())).id(APPEND_FEDORA_3_OBJ_INFO)
                .choice()
                    .when(simple("${property.objectState} == 'A'"))
                        .choice()
                            .when(simple("${property.eventType} == 'create' or ${property.eventType} == 'update'"))
                                .to("direct:sitemap_create_url")
                            .when(simple("${property.eventType} == 'delete'"))
                                .to("direct:sitemap_delete_url")
                    .otherwise()
                        .to("mock:otherwise");

        // This route is for fedora 3 only.
        from(FEDORA_3_OBJECTINFO)
                .routeId(FEDORA_3_OBJECTINFO_ID)
//                .setProperty("encodedpid", jsonpath("$.encodedpid"))
                .recipientList(simple("http4://{{fedora.service.url}}/fedora/objects/$['org.fcrepo.jms.identifier']?format=xml"));

        from(DIRECT_CREATE_URL)
                .routeId(SITEMAP_CREATE_URL_ID)
                .log("create_url (json): ${body}")
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.POST))
                .setHeader(Exchange.CHARSET_NAME, constant("UTF-8"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .throttle(10)
                .to(SITEMAP_SERVICE_CREATE_URL);

        // route to update sitemap via pid's (post qucosa-ID's (qucosa:12345) to kafka topic "pidupdate")
//        from("kafka:pidupdate?groupId=bulkinsert")
//                .routeId(KAFKA_BULK_INSERT_ROUTE)
//                // set/get method/tenant/pid/encodedpid
//                .process(new SetupJsonForBulkInsert())
//                .to("kafka:sitemap_feeder");

        // route to update sitemap via pid's (post qucosa-ID's (qucosa:12345) to kafka topic "pidupdate")
//        from("kafka:piddelete?groupId=bulkdelete")
//                .routeId(KAFKA_BULK_DELETE_ROUTE)
//                // set/get method/tenant/pid/encodedpid
//                .process(new SetupJsonForBulkDelete())
//                .to("kafka:sitemap_feeder");

//        from("direct:sitemap_delete_urlset")
//                .routeId("deleteUrlsetRoute")
//                .setProperty("tenant", jsonpath("$.tenant_urlset"))
//                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.DELETE))
//                .recipientList(simple("http4://{{sitemap.service.url}}/urlsets/${exchangeProperty.tenant}?throwExceptionOnFailure=false"));
//
//        from("direct:sitemap_delete_url")
//                .routeId("deleteUrlRoute")
//                .setProperty("tenant", jsonpath("$.tenant_urlset"))
//                .process(new UrlFormatProcessor())
//                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.DELETE))
//                .setHeader(Exchange.CHARSET_NAME, constant("UTF-8"))
//                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
//                .recipientList(simple("http4://{{sitemap.service.url}}/urlsets/${exchangeProperty.tenant}/deleteurl?throwExceptionOnFailure=false"));

//        from("direct:sitemap_modify_url_lastmod")
//                .routeId("modifyUrlRoute")
//                .setProperty("tenant", jsonpath("$.tenant_urlset"))
//                .process(new UrlFormatProcessor())
//                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.PUT))
//                .setHeader(Exchange.CHARSET_NAME, constant("UTF-8"))
//                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
//                .recipientList(simple("http4://{{sitemap.service.url}}/urlsets/${exchangeProperty.tenant}?throwExceptionOnFailure=false"));
    }

    private List<Tenant> tenants() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        List<Tenant> tenants = objectMapper.readValue(getClass().getResourceAsStream(getContext().resolvePropertyPlaceholders("{{conf.path}}") + "tenant.json"),
                objectMapper.getTypeFactory().constructCollectionType(List.class, Tenant.class));
        return tenants;
    }
}
