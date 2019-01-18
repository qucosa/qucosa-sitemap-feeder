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
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ActiveMqRoute extends RouteBuilder {
    @Value("#{${tenantShort.map}}")
    private Map<String, String> tenantShortMap;

    @Value("#{${tenantLong.map}}")
    private Map<String, String> tenantLongMap;

    @Override
    public void configure() {
        UrlsetFormatProcessor urlsetFormatProcessor = new UrlsetFormatProcessor();
        UrlFormatProcessor urlFormatProcessor = new UrlFormatProcessor();
        AMQMessageProcessor amqMessageProcessor = new AMQMessageProcessor();
        AggregationStrategy appendUrlsetName = new AppendTenantStrategy(tenantShortMap, tenantLongMap);
        SetupJsonForBulkInsert jsonForBulkInsert = new SetupJsonForBulkInsert();
        SetupJsonForBulkDelete jsonForBulkDelete = new SetupJsonForBulkDelete();

        // setup kafka component with the brokers
        KafkaComponent kafka = new KafkaComponent();
        kafka.setBrokers("{{kafka.broker.host}}:{{kafka.broker.port}}");
        getContext().addComponent("kafka", kafka);

        // Transport updated PIDs to Kafka topic
        from("activemq:topic:fedora.apim.update")
                .id("fcrepo_updates")
                // XML-to-JSON-mapping of relevant information
                .process(amqMessageProcessor)
                .multicast()
//                    .to("kafka:fcrepo_updates")
                    .to("kafka:sitemap_feeder")
        ;

        // route to update sitemap via pid's (post qucosa-ID's (qucosa:12345) to kafka topic "pidupdate")
        from("kafka:pidupdate")
                .id("pidupdate")
                // set/get method/tenant/pid/encodedpid
                .process(jsonForBulkInsert)
                .to("kafka:sitemap_feeder")
        ;

        // route to update sitemap via pid's (post qucosa-ID's (qucosa:12345) to kafka topic "pidupdate")
        from("kafka:piddelete")
                .id("piddelete")
                // set/get method/tenant/pid/encodedpid
                .process(jsonForBulkDelete)
                .to("kafka:sitemap_feeder")
        ;

        from("kafka:sitemap_feeder")
                .id("sitemap_feeder")
                // appends tenant (urlset-name) to object-information in body
                .enrich("direct:objectinfo", appendUrlsetName)
                .choice()
                .when().jsonpath("$.[?(@.objectState != 'I')]")
                    .when().jsonpath("$.[?(@.objectState != 'D')]")
                        .when().jsonpath("$.[?(@.method == 'ingest')]")
                            .multicast()
                            .parallelProcessing(false)
                            .to("direct:sitemap_create_urlset", "direct:sitemap_create_url")
                            .endChoice()
                        .when().jsonpath("$.[?(@.method == 'addDatastream')]")
                            .multicast()
                            .parallelProcessing(false)
                            .to("direct:sitemap_modify_url_lastmod")
                            .endChoice()
                        .when().jsonpath("$.[?(@.method == 'purgeObject')]")
                            .multicast()
                            .parallelProcessing(false)
                            .to("direct:sitemap_delete_url")
                            .endChoice()
                        .when().jsonpath("$.[?(@.method == 'modifyObject')]")
                            .multicast()
                            .parallelProcessing(false)
                            .to("direct:sitemap_modify_url_lastmod")
                            .endChoice()
                    .endChoice()
                .endChoice()
                .end();

        from("direct:objectinfo")
                .setProperty("encodedpid", jsonpath("$.encodedpid"))
                .recipientList(simple("http4://{{fedora.host}}:{{fedora.port}}/fedora/objects/${exchangeProperty.encodedpid}?format=xml"));

        // Sitemap update
        from("direct:sitemap_create_urlset")
                // set json-format for urlset's with tenantname (urlset-uri)
                .process(urlsetFormatProcessor)
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.POST))
                .setHeader(Exchange.CHARSET_NAME, constant("UTF-8"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .throttle(10)
                // throwExceptionOnFailure set to false to disable camel from throwing HttpOperationFailedException
                // on response-codes 300+
                .recipientList(simple("http4://{{sitemap.host}}:{{sitemap.port}}/urlsets?throwExceptionOnFailure=false"));

        from("direct:sitemap_create_url")
                // create urlset if missing
                .setProperty("tenant", jsonpath("$.tenant_urlset"))
                .process(urlFormatProcessor)
                .log("create_url (json): ${body}")
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.POST))
                .setHeader(Exchange.CHARSET_NAME, constant("UTF-8"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .setHeader("pid", exchangeProperty("pid"))
                .throttle(10)
                .recipientList(simple("http4://{{sitemap.host}}:{{sitemap.port}}/urlsets/${exchangeProperty.tenant}"));

        from("direct:sitemap_delete_urlset")
                .setProperty("tenant", jsonpath("$.tenant_urlset"))
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.DELETE))
                .recipientList(simple("http4://{{sitemap.host}}:{{sitemap.port}}/urlsets/${exchangeProperty.tenant}?throwExceptionOnFailure=false"));

        from("direct:sitemap_delete_url")
                .setProperty("tenant", jsonpath("$.tenant_urlset"))
                .process(urlFormatProcessor)
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.DELETE))
                .setHeader(Exchange.CHARSET_NAME, constant("UTF-8"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .recipientList(simple("http4://{{sitemap.host}}:{{sitemap.port}}/urlsets/${exchangeProperty.tenant}/deleteurl?throwExceptionOnFailure=false"));

        from("direct:sitemap_modify_url_lastmod")
                .setProperty("tenant", jsonpath("$.tenant_urlset"))
                .process(urlFormatProcessor)
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.PUT))
                .setHeader(Exchange.CHARSET_NAME, constant("UTF-8"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .recipientList(simple("http4://{{sitemap.host}}:{{sitemap.port}}/urlsets/${exchangeProperty.tenant}?throwExceptionOnFailure=false"));
    }
}
