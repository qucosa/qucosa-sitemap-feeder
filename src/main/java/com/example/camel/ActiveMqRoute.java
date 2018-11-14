package com.example.camel;

import com.example.camel.camelprocessors.AMQMessageProcessor;
import com.example.camel.camelprocessors.SetPropertyEncodedpid;
import com.example.camel.camelprocessors.UrlFormatProcessor;
import com.example.camel.camelprocessors.UrlsetFormatProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.component.http4.HttpMethods;
import org.apache.camel.component.kafka.KafkaComponent;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

@Component
public class ActiveMqRoute extends RouteBuilder {

    @Override
    public void configure() {
        UrlsetFormatProcessor urlsetFormatProcessor = new UrlsetFormatProcessor();
        UrlFormatProcessor urlFormatProcessor = new UrlFormatProcessor();
        AMQMessageProcessor amqMessageProcessor = new AMQMessageProcessor();
        AggregationStrategy appendUrlsetName = new AppendUrlsetNameStrategy();
        SetPropertyEncodedpid setPropertyEncodedpid = new SetPropertyEncodedpid();

        Namespaces ns = new Namespaces("atom", "http://www.w3.org/2005/Atom")
                .add("mets", "http://www.loc.gov/METS/");

        // setup kafka component with the brokers
        KafkaComponent kafka = new KafkaComponent();
        kafka.setBrokers("localhost:9092");
        getContext().addComponent("kafka", kafka);

        // Transport updated PIDs to Kafka topic
        from("activemq:topic:fedora.apim.update")
                .id("fcrepo_updates")
                // XML-to-JSON-mapping of relevant information
                .process(amqMessageProcessor)
                .to("kafka:fcrepo_updates", "kafka:sitemap_feeder");

        // Obtain and post METS XML to Kafka topic
        from("kafka:fcrepo_updates?groupId=mets_dissemination")
                .id("mets_update")
                // aus body die methode etc. ziehen
                .transform(jsonpath("$.pid"))
                .resequence().body().timeout(TimeUnit.SECONDS.toMillis(5))
                .setProperty("pid", body())
                .setHeader(Exchange.HTTP_QUERY, simple("pid=${exchangeProperty[pid]}"))
                .throttle(1)
                .to("http4://sdvcmr-app03:8080/mets")
                .convertBodyTo(Document.class, "UTF-8")
                .setHeader(KafkaConstants.KEY, exchangeProperty("pid"))
                .setProperty("tenant", xpath("//mets:mets/mets:metsHdr/mets:agent/mets:name").namespaces(ns))
                .to("kafka:mets_updates")
        ;

        // TODO: statt METS: http://sdvcmr-app03:8080/fedora/objects/qucosa:70489?format=xml
        // exchange enrichen (oben methoden, hier tenant aus objectProfile/ObjOwnerId/text()

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
                    .to("direct:sitemap_create_or_modify_urlset", "direct:sitemap_create_url")
                    .endChoice()
                .when().jsonpath("$.[?(@.method == 'purgeObject')]")
                    // needs mets (tenantname for URL)
                    .multicast()
                    .parallelProcessing(false)
                    .to("direct:sitemap_create_or_modify_urlset", "direct:sitemap_delete_url")
                    .endChoice()
                .when().jsonpath("$.[?(@.method == 'modifyObject')]")
                    // notwendigkeit überprüfen
                    .multicast()
                    .parallelProcessing(false)
//                    .to("direct:sitemap_create_urlset", "direct:sitemap_modify_url")
                    .to("direct:sitemap_create_urlset", "direct:sitemap_create_url")
                    .endChoice()
                .when().jsonpath("$.[?(@.method == 'setDatastreamState')]")
                    // notwendigkeit überprüfen
                    .multicast()
                    .parallelProcessing(false)
                    .to("direct:sitemap_modify_url")
                    .endChoice()
                .end();

        from("direct:objectinfo")
                .process(setPropertyEncodedpid)
                .recipientList(simple("http4://sdvcmr-app03:8080/fedora/objects/${exchangeProperty.encodedpid}?format=xml"));

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
                .to("http4://localhost:8090/urlsets?throwExceptionOnFailure=false");

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
                .recipientList(simple("http4://localhost:8090/urlsets/${exchangeProperty.tenant}"));

        //TODO delete-routes
        from("direct:sitemap_delete_urlset")
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.DELETE))
                // get url for qucosa id
                .process(urlsetFormatProcessor)
                .to("http4://localhost:8090/urlsets/${exchangeProperty.tenant}?throwExceptionOnFailure=false");

        from("direct:sitemap_delete_url")
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.DELETE))
                // body
                .process(urlFormatProcessor)
                .to("http4://localhost:8090/urlsets/${exchangeProperty.tenant}/deleteurl?throwExceptionOnFailure=false");

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
                .to("http4://localhost:8090/urlsets?throwExceptionOnFailure=false")
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
    }
}
