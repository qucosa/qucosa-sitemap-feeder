package de.qucosa.camel;

import de.qucosa.camel.camelprocessors.AMQMessageProcessor;
import de.qucosa.camel.camelprocessors.SetupJsonForBulkDelete;
import de.qucosa.camel.camelprocessors.SetupJsonForBulkInsert;
import de.qucosa.camel.camelprocessors.UrlFormatProcessor;
import de.qucosa.camel.camelprocessors.UrlsetFormatProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpMethods;
import org.apache.camel.component.kafka.KafkaComponent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

import static de.qucosa.camel.utils.RouteIds.ACTIVEMQ_ROUTE;
import static de.qucosa.camel.utils.RouteIds.APPEND_FEDORA_OBJ_INFO;
import static de.qucosa.camel.utils.RouteIds.HTTP_ADD_DATASTREAM_ID;
import static de.qucosa.camel.utils.RouteIds.HTTP_INGEST_ID;
import static de.qucosa.camel.utils.RouteIds.HTTP_MODIFY_OBJECT_ID;
import static de.qucosa.camel.utils.RouteIds.HTTP_PURGE_OBJECT_ID;
import static de.qucosa.camel.utils.RouteIds.KAFKA_BULK_DELETE_ROUTE;
import static de.qucosa.camel.utils.RouteIds.KAFKA_BULK_INSERT_ROUTE;
import static de.qucosa.camel.utils.RouteIds.SITEMAP_FEEDER_ROUTE;
@Component
public class ActiveMqRoute extends RouteBuilder {
    @Value("#{${tenantShort.map}}")
    private Map<String, String> tenantShortMap;

    @Value("#{${tenantLong.map}}")
    private Map<String, String> tenantLongMap;

    @Override
    public void configure() {
        KafkaComponent kafka = new KafkaComponent();
        kafka.setBrokers("{{kafka.broker.host}}");
        getContext().addComponent("kafka", kafka);

        // Transport updated PIDs to Kafka topic
        from("activemq:topic:fedora.apim.update")
                .routeId(ACTIVEMQ_ROUTE)
                // XML-to-JSON-mapping of relevant information
                .process(new AMQMessageProcessor())
                .to("kafka:sitemap_feeder?groupId=modifysitemap");

        // route to update sitemap via pid's (post qucosa-ID's (qucosa:12345) to kafka topic "pidupdate")
        from("kafka:pidupdate?groupId=bulkinsert")
                .routeId(KAFKA_BULK_INSERT_ROUTE)
                // set/get method/tenant/pid/encodedpid
                .process(new SetupJsonForBulkInsert())
                .to("kafka:sitemap_feeder?groupId=modifysitemap");

        // route to update sitemap via pid's (post qucosa-ID's (qucosa:12345) to kafka topic "pidupdate")
        from("kafka:piddelete?groupId=bulkdelete")
                .routeId(KAFKA_BULK_DELETE_ROUTE)
                // set/get method/tenant/pid/encodedpid
                .process(new SetupJsonForBulkDelete())
                .to("kafka:sitemap_feeder?groupId=modifysitemap");

        from("kafka:sitemap_feeder?groupId=modifysitemap")
                .routeId(SITEMAP_FEEDER_ROUTE)
                // appends tenant (urlset-name) and objectState to JSON-body
                .enrich("direct:objectinfo", new AppendFedoraObjectInfo(tenantShortMap, tenantLongMap))
                .id(APPEND_FEDORA_OBJ_INFO)
                .choice()
                    .when().jsonpath("$.[?(@.objectState == 'I')]").to("mock:inactive_doc")
                    .when().jsonpath("$.[?(@.objectState == 'D')]").to("mock:deleted_doc")
                    .when().jsonpath("$.[?(@.objectState == 'A')]")
                        .choice()
                            .when().jsonpath("$.[?(@.method == 'addDatastream')]")
                                .to("direct:sitemap_modify_url_lastmod")
                                .id(HTTP_ADD_DATASTREAM_ID)
                            .when().jsonpath("$.[?(@.method == 'ingest')]")
                                .multicast()
                                .parallelProcessing(false)
                                .to("direct:sitemap_create_urlset", "direct:sitemap_create_url")
                                .id(HTTP_INGEST_ID)
                                .endChoice() // needed for multicast
                            .when().jsonpath("$.[?(@.method == 'purgeObject')]")
                                .to("direct:sitemap_delete_url")
                                .id(HTTP_PURGE_OBJECT_ID)
                            .when().jsonpath("$.[?(@.method == 'modifyObject')]")
                                .to("direct:sitemap_modify_url_lastmod")
                                .id(HTTP_MODIFY_OBJECT_ID)
                        .endChoice()
                .endChoice();

        from("direct:objectinfo")
                .setProperty("encodedpid", jsonpath("$.encodedpid"))
                .recipientList(simple("http4://{{fedora.host}}:{{fedora.port}}/fedora/objects/${exchangeProperty.encodedpid}?format=xml"));

        // Sitemap update
        from("direct:sitemap_create_urlset")
                .routeId("createUrlsetRoute")
                // set json-format for urlset's with tenantname (urlset-uri)
                .process(new UrlsetFormatProcessor())
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.POST))
                .setHeader(Exchange.CHARSET_NAME, constant("UTF-8"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .throttle(10)
                // throwExceptionOnFailure set to false to disable camel from throwing HttpOperationFailedException
                // on response-codes 300+
                .recipientList(simple("http4://{{sitemap.host}}:{{sitemap.port}}/urlsets?throwExceptionOnFailure=false"));

        from("direct:sitemap_create_url")
                .routeId("createUrlRoute")
                // create urlset if missing
                .setProperty("tenant", jsonpath("$.tenant_urlset"))
                .process(new UrlFormatProcessor())
                .log("create_url (json): ${body}")
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.POST))
                .setHeader(Exchange.CHARSET_NAME, constant("UTF-8"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .setHeader("pid", exchangeProperty("pid"))
                .throttle(10)
                .recipientList(simple("http4://{{sitemap.host}}:{{sitemap.port}}/urlsets/${exchangeProperty.tenant}"));

        from("direct:sitemap_delete_urlset")
                .routeId("deleteUrlsetRoute")
                .setProperty("tenant", jsonpath("$.tenant_urlset"))
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.DELETE))
                .recipientList(simple("http4://{{sitemap.host}}:{{sitemap.port}}/urlsets/${exchangeProperty.tenant}?throwExceptionOnFailure=false"));

        from("direct:sitemap_delete_url")
                .routeId("deleteUrlRoute")
                .setProperty("tenant", jsonpath("$.tenant_urlset"))
                .process(new UrlFormatProcessor())
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.DELETE))
                .setHeader(Exchange.CHARSET_NAME, constant("UTF-8"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .recipientList(simple("http4://{{sitemap.host}}:{{sitemap.port}}/urlsets/${exchangeProperty.tenant}/deleteurl?throwExceptionOnFailure=false"));

        from("direct:sitemap_modify_url_lastmod")
                .routeId("modifyUrlRoute")
                .setProperty("tenant", jsonpath("$.tenant_urlset"))
                .process(new UrlFormatProcessor())
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.PUT))
                .setHeader(Exchange.CHARSET_NAME, constant("UTF-8"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .recipientList(simple("http4://{{sitemap.host}}:{{sitemap.port}}/urlsets/${exchangeProperty.tenant}?throwExceptionOnFailure=false"));
    }
}
