package com.example.camel;

import com.example.camel.camelprocessors.AMQMessageProcessor;
import com.example.camel.camelprocessors.SetupJsonForBulkInsert;
import com.example.camel.camelprocessors.UrlFormatProcessor;
import com.example.camel.camelprocessors.UrlsetFormatProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.component.http4.HttpMethods;
import org.apache.camel.component.kafka.KafkaComponent;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ActiveMqRoute extends RouteBuilder {
    @Value("#{${tenant.map}}")
    private Map<String, String> tenantmap;

    @Override
    public void configure() {
        UrlsetFormatProcessor urlsetFormatProcessor = new UrlsetFormatProcessor();
        UrlFormatProcessor urlFormatProcessor = new UrlFormatProcessor();
        AMQMessageProcessor amqMessageProcessor = new AMQMessageProcessor();
        AggregationStrategy appendUrlsetName = new AppendUrlsetNameStrategy(tenantmap);
        SetupJsonForBulkInsert jsonForBulkInsert = new SetupJsonForBulkInsert();

        Namespaces ns = new Namespaces("atom", "http://www.w3.org/2005/Atom")
                .add("mets", "http://www.loc.gov/METS/");

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


        from("kafka:sitemap_feeder")
                .id("sitemap_feeder")
                .log("sitemap_feeder body: ${body}")
                // appends tenant (urlset-name) to object-information in body
                .enrich("direct:objectinfo", appendUrlsetName)
                .log("sitemap_feeder body after enrich: ${body}")
                .choice()
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
                    .to("direct:sitemap_delete_url", "direct:sitemap_update_urlset_lastmod")
                    .endChoice()
                .when().jsonpath("$.[?(@.method == 'modifyObject')]")
                    .multicast()
                    .parallelProcessing(false)
                    .to("direct:sitemap_modify_url_lastmod")
                    .endChoice()
                .end();

        from("direct:objectinfo")
                .setProperty("encodedpid", jsonpath("$.encodedpid"))
                .recipientList(simple("http4://{{fedora.host}}:{{fedora.port}}/fedora/objects/${exchangeProperty.encodedpid}?format=xml"));

        // Sitemap update
        from("direct:sitemap_create_urlset")
                // set json-format for urlset's with tenantname (urlset-uri)
                .process(urlsetFormatProcessor)
                .log("create_urlset (json): ${body}")
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.POST))
                .setHeader(Exchange.CHARSET_NAME, constant("UTF-8"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .throttle(10)
                // throwExceptionOnFailure set to false to disable camel from throwing HttpOperationFailedException
                // on response-codes 300+
                .to("http4://{{sitemap.host}}:{{sitemap.port}}/urlsets?throwExceptionOnFailure=false");

        from("direct:sitemap_create_url")
                // create urlset if missing
                .setProperty("tenant", jsonpath("$.tenant"))
                .process(urlFormatProcessor)
                .log("create_url (json): ${body}")
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.POST))
                .setHeader(Exchange.CHARSET_NAME, constant("UTF-8"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .setHeader("pid", exchangeProperty("pid"))
                .throttle(10)
                .recipientList(simple("http4://{{sitemap.host}}:{{sitemap.port}}/urlsets/${exchangeProperty.tenant}"));

        from("direct:sitemap_delete_urlset")
                .setProperty("tenant", jsonpath("$.tenant"))
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.DELETE))
                .to("http4://{{sitemap.host}}:{{sitemap.port}}/urlsets/${exchangeProperty.tenant}?throwExceptionOnFailure=false");

        from("direct:sitemap_delete_url")
                .setProperty("tenant", jsonpath("$.tenant"))
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.DELETE))
                .process(urlFormatProcessor)
                .to("http4://{{sitemap.host}}:{{sitemap.port}}/urlsets/${exchangeProperty.tenant}/deleteurl?throwExceptionOnFailure=false");

        from("direct:sitemap_modify_url_lastmod")
                .setProperty("tenant", jsonpath("$.tenant"))
                .process(urlFormatProcessor)
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.PUT))
                .setHeader(Exchange.CHARSET_NAME, constant("UTF-8"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .to("http4://{{sitemap.host}}:{{sitemap.port}}/urlsets/${exchangeProperty.tenant}?throwExceptionOnFailure=false");

        /*
        //TODO modify-route
        from("direct:sitemap_create_or_modify_urlset")
                // create urlset if missing
                .setProperty("pid", header(KafkaConstants.KEY))
                .process(urlsetFormatProcessor)
                .log("tenantname: ${body}")
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.POST))
                .setHeader(Exchange.CHARSET_NAME, constant("UTF-8"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .throttle(10)
                .to("http4://{{sitemap.host}}:{{sitemap.port}}/urlsets?throwExceptionOnFailure=false")
                .process(exchange -> {
                    HttpServletResponse response = exchange.getIn().getBody(HttpServletResponse.class);
                    int statuscode = response.getStatus();

                    if (statuscode == 404) {
                        exchange.setProperty("urlset_available", true);
                    }
                    // urlset already created, statuscode 208 = already reported
                    if (statuscode == 208) {

                    } else {
                        exchange.setProperty("urlset_available", false);
                    }
                })
                .choice()
                    .when().simple("${exchangeProperty.urlset_available} == 'true'")
                .multicast()
                .parallelProcessing(false)
                .to("direct:sitemap_create_urlset", "direct:sitemap_create_url")
                .endChoice()
        ;
        */

        /*
        // Send XML to ExistDB
        from("kafka:mets_updates?groupId=existdb_feeder")
                .id("existdb_feeder")
                .setHeader("pid", header(KafkaConstants.KEY).regexReplaceAll(":", "-"))
                .setHeader(Exchange.HTTP_PATH, simple("{{existdb.document.path}}/${header[pid]}.mets.xml"))
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.PUT))
                .setHeader(Exchange.CHARSET_NAME, constant("UTF-8"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/xml"))
                .throttle(10)
                .to("http4://{{existdb.host}}:{{existdb.port}}/exist/rest" +
                        "?authUsername={{existdb.user}}" +
                        "&authPassword={{existdb.pw}}")
                .log("Updated ${header.CamelHttpPath}")
        ;
        +/

        /*
        // Obtain and post METS XML to Kafka topic
        from("kafka:fcrepo_updates?groupId=mets_dissemination")
                .id("mets_update")
                .transform(jsonpath("$.pid"))
                .resequence().body().timeout(TimeUnit.SECONDS.toMillis(5))
                .setProperty("pid", body())
                .setHeader(Exchange.HTTP_QUERY, simple("pid=${exchangeProperty[pid]}"))
                .throttle(1)
                .to("http4://{{fedora.host}}:{{fedora.port}}/mets")
                .convertBodyTo(Document.class, "UTF-8")
                .setHeader(KafkaConstants.KEY, exchangeProperty("pid"))
                .setProperty("tenant", xpath("//mets:mets/mets:metsHdr/mets:agent/mets:name").namespaces(ns))
                .to("kafka:mets_updates")
        ;
        */
    }
}
