package de.qucosa.camel;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.kafka.KafkaComponent;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static de.qucosa.camel.utils.RouteIds.ACTIVEMQ_ROUTE;
import static de.qucosa.camel.utils.RouteIds.APPEND_FEDORA_OBJ_INFO;
import static de.qucosa.camel.utils.RouteIds.SITEMAP_FEEDER_ROUTE;

@DisplayName("Test dataflow activeMQ -> kafka -> sitemap service.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(value = MethodOrderer.OrderAnnotation.class)
@Testcontainers
public class SitemapFeederRoutesTest {
    private final DefaultCamelContext camelContext = new DefaultCamelContext();

    @Container
    private static final KafkaContainer kafkaCon = new KafkaContainer()
            .withCreateContainerCmdModifier(
                    createContainerCmd -> createContainerCmd.withName("qucosa-sitemap-kafka")

            );

    private static String AMQ_FILE_PATH = SitemapFeederRoutesTest.class.getResource("/jms/").getPath();

    @BeforeAll
    public void setUp() throws Exception {
        kafkaCon.start();
        kafkaCon.execInContainer("/bin/sh", "-c", "kafka-topics --zookeeper localhost:2181 --partitions=1 --replication-factor=1 --create --topic sitemap_feeder");
        kafkaCon.execInContainer("/bin/sh", "-c", "kafka-topics --zookeeper localhost:2181 --partitions=1 --replication-factor=1 --create --topic pidupdate");

        PropertiesComponent pc = (PropertiesComponent) camelContext.getComponent("properties");
        pc.setLocation("classpath:application-test.properties");
        camelContext.addRoutes(new SitemapFeederRoutes());

        KafkaComponent kafkaComponent = (KafkaComponent) camelContext.getComponent("kafka");
        kafkaComponent.setBrokers(kafkaCon.getBootstrapServers());

        camelContext.getRouteDefinition(ACTIVEMQ_ROUTE).adviceWith(camelContext, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("file://" + AMQ_FILE_PATH + "?fileName=ingest.xml&noop=true");
                weaveByToUri("kafka:sitemap_feeder").after().to("mock:ingest");
            }
        });

//        camelContext.getRouteDefinition(SITEMAP_FEEDER_ROUTE).adviceWith(camelContext, new AdviceWithRouteBuilder() {
//            @Override
//            public void configure() throws Exception {
//                weaveById(APPEND_FEDORA_OBJ_INFO)
//                        .replace()
//                        .to("");
//            }
//        });
    }

    @Test
    public void inittest() throws Exception {
        MockEndpoint ingest = camelContext.getEndpoint("mock:ingest", MockEndpoint.class);
        camelContext.start();
        ingest.expectedMessageCount(1);
        ProducerTemplate template = camelContext.createProducerTemplate();
        template.sendBody("kafka:sitemap_feeder", getClass().getResourceAsStream("jms/ingest.xml"));
        ingest.assertIsSatisfied();
    }

    @AfterAll
    public void shutdown() {
        kafkaCon.stop();
    }
}
