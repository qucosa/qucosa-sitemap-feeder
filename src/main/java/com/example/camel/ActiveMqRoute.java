package com.example.camel;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.component.http4.HttpMethods;
import org.apache.camel.component.kafka.KafkaComponent;
import org.apache.camel.component.kafka.KafkaConstants;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import java.util.concurrent.TimeUnit;

@Component
public class ActiveMqRoute extends RouteBuilder {

    @Override
    public void configure() {
        MetsToUrlsetProcessor metsToUrlsetProcessor = new MetsToUrlsetProcessor();
        MetsToUrlProcessor metsToUrlProcessor = new MetsToUrlProcessor();

        Namespaces ns = new Namespaces("atom", "http://www.w3.org/2005/Atom")
                .add("mets", "http://www.loc.gov/METS/");

        // setup kafka component with the brokers
        KafkaComponent kafka = new KafkaComponent();
        kafka.setBrokers("localhost:9092");
        getContext().addComponent("kafka", kafka);

        // Transport updated PIDs to Kafka topic
        from("activemq:topic:fedora.apim.updates")
                .id("fcrepo_updates")
                .setProperty("method", xpath("/atom:entry/atom:title[@type='text']/text()").namespaces(ns))
                .transform(xpath("/atom:entry/atom:summary[@type='text']/text()", String.class)
                        .namespaces(ns))
                .to("kafka:fcrepo_updates");

        // Obtain and post METS XML to Kafka topic
        from("kafka:fcrepo_updates?groupId=mets_dissemination")
                .id("mets_update")
                .log("body: ${body}")
                .resequence().body().timeout(TimeUnit.SECONDS.toMillis(5))
                .setProperty("pid", body())
                .setHeader(Exchange.HTTP_QUERY, simple("pid=${exchangeProperty[pid]}"))
                .throttle(1)
                .to("http4://sdvcmr-app03:8080/mets")
//                .convertBodyTo(String.class, "UTF-8")
                .convertBodyTo(Document.class, "UTF-8")
                .setHeader(KafkaConstants.KEY, exchangeProperty("pid"))
                .setProperty("tenant", xpath("//mets:mets/mets:metsHdr/mets:agent/mets:name").namespaces(ns))
                .to("kafka:mets_updates");

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

        from("kafka:mets_updates?groupId=sitemap_feeder")
                .id("sitemap_feeder")
//                .noAutoStartup()
                .setProperty("pid", header(KafkaConstants.KEY))
                .choice()
                .when().simple("${exchangeProperty.method} == 'ingest'")
                    .multicast()
                    .parallelProcessing(false)
                    .to("direct:sitemap_create_urlset", "direct:sitemap_create_url")
                    .endChoice()
                .when().simple("${exchangeProperty.method} == 'addDatastream'")
                    .multicast()
                    .parallelProcessing(false)
                    .to("direct:sitemap_create_or_modify_urlset", "direct:sitemap_create_url")
                    .endChoice()
                .when().simple("${exchangeProperty.method} == 'purgeObject'")
                    // needs mets (tenantname for URL)
                    .multicast()
                    .parallelProcessing(false)
                    .to("direct:sitemap_create_or_modify_urlset", "direct:sitemap_delete_url")
                    .endChoice()
                .when().simple("${exchangeProperty.method} == 'modifyObject'")
                    // notwendigkeit überprüfen
                    .multicast()
                    .parallelProcessing(false)
                    .to("direct:sitemap_modify_urlset", "direct:sitemap_modify_url")
                    .endChoice()
                .when().simple("${exchangeProperty.method} == 'setDatastreamState'")
                    // notwendigkeit überprüfen
                    .multicast()
                    .parallelProcessing(false)
                    .to("direct:sitemap_modify_url")
                    .endChoice()
                .end();
        //                .when().simple("${exchangeProperty.method} == 'modifyDatastreamByReference'")
        //                .to("direct:updateDatastreamIndex")
        //                .when().simple("${exchangeProperty.method} == 'modifyDatastreamByValue'")
        //                .to("direct:updateDatastreamIndex")
        //                .when().simple("${exchangeProperty.method} == 'purgeDatastream'")
        //                .to("direct:deleteDatastreamIndex")

        // Sitemap update

        from("direct:sitemap_create_urlset")
                // create urlset if missing
                .setProperty("pid", header(KafkaConstants.KEY))
                .process(metsToUrlsetProcessor)
                .log("tenantname: ${body}")
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.POST))
                .setHeader(Exchange.CHARSET_NAME, constant("UTF-8"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .throttle(10)
                .to("http4://localhost:8090/urlsets");

        from("direct:sitemap_create_url")
                // create urlset if missing
                .setProperty("pid", header(KafkaConstants.KEY))
                .process(metsToUrlProcessor)
                .log("tenantname: ${body}")
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.POST))
                .setHeader(Exchange.CHARSET_NAME, constant("UTF-8"))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .setHeader("pid", exchangeProperty("pid"))
                .throttle(10)
                // exchange property set in createUrlsetProcessor
                .recipientList(simple("http4://localhost:8090/urlsets/${exchangeProperty.uri}"));

        //TODO delete-routes
        from("direct:sitemap_delete_urlset")
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.DELETE))
                // get url for qucosa id
                .process(metsToUrlsetProcessor)
                .to("http4://localhost:8090/urlsets/${exchangeProperty.tenant}");

        from("direct:sitemap_delete_url")
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.DELETE))
                // body
                .process(metsToUrlProcessor)
//                .marshal(jsonUrlFormat)
                .to("http4://localhost:8090/urlsets/${exchangeProperty.tenant}/deleteurl");

        //TODO modify-route

    }
}
