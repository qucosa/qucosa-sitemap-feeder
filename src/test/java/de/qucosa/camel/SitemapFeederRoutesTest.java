package de.qucosa.camel;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.qucosa.camel.model.Url;
import de.qucosa.camel.routebuilders.SitemapFeederRoutes;
import de.qucosa.camel.utils.DateTimeConverter;
import de.qucosa.data.KafkaTopicData;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.kafka.KafkaComponent;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.Properties;

import static de.qucosa.camel.config.EndpointUris.DIRECT_CREATE_URL;
import static de.qucosa.camel.config.RouteIds.APPEND_FEDORA_3_OBJ_INFO;
import static de.qucosa.camel.config.RouteIds.KAFKA_SITEMAP_CONSUMER_ID;

@DisplayName("Test dataflow activeMQ -> kafka -> sitemap service.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(value = MethodOrderer.OrderAnnotation.class)
@Testcontainers
public class SitemapFeederRoutesTest {

    public static String objState;

    public static String eventType;

    private final static DefaultCamelContext camelContext = new DefaultCamelContext();

    private static KafkaProducer<String, String> kafkaProducer;

    @Container
    private static final KafkaContainer kafkaCon = new KafkaContainer()
            .withCreateContainerCmdModifier(
                    createContainerCmd -> createContainerCmd.withName("qucosa-sitemap-kafka")

            );

    private static String AMQ_FILE_PATH = SitemapFeederRoutesTest.class.getResource("/jms/").getPath();

    @BeforeAll
    public void allSetUp() throws IOException, InterruptedException {
        kafkaCon.start();
        kafkaCon.execInContainer("/bin/sh", "-c", "kafka-topics --zookeeper localhost:2181 --partitions=1 --replication-factor=1 --create --topic pidupdate");
        kafkaCon.execInContainer("/bin/sh", "-c", "kafka-topics --zookeeper localhost:2181 --partitions=1 --replication-factor=1 --create --topic piddelete");
        kafkaCon.execInContainer("/bin/sh", "-c", "kafka-topics --zookeeper localhost:2181 --partitions=1 --replication-factor=1 --create --topic service_events");

        PropertiesComponent pc = (PropertiesComponent) camelContext.getComponent("properties");
        pc.setLocation("classpath:application-test.properties");

        kafkaProducer = kafkaProducer();
    }

    @BeforeEach
    public void setUp() throws Exception {
        camelContext.addRoutes(new SitemapFeederRoutes());

        KafkaComponent kafkaComponent = (KafkaComponent) camelContext.getComponent("kafka");
        kafkaComponent.setBrokers(kafkaCon.getBootstrapServers());
    }

    @Test
    @Order(1)
    @DisplayName("Ingest the sitemap url.")
    public void ingest() throws Exception {
        kafkaProducer.send(producerRecord(KafkaTopicData.JSON_CREATE_EVENT));
        objState = "A";
        eventType = "create";

        camelContext.getRouteDefinition(KAFKA_SITEMAP_CONSUMER_ID).adviceWith(camelContext, createOrUpdate());

        MockEndpoint saveUrl = camelContext.getEndpoint("mock:saveUrl", MockEndpoint.class);
        saveUrl.expectedMessageCount(1);
        camelContext.start();
        saveUrl.assertIsSatisfied();
        camelContext.stop();
    }

    @Test
    @Order(2)
    @DisplayName("Update the sitemap url.")
    public void update() throws Exception {
        kafkaProducer.send(producerRecord(KafkaTopicData.JSON_UPDATE_EVENT));
        objState = "A";
        eventType = "update";

        camelContext.getRouteDefinition(KAFKA_SITEMAP_CONSUMER_ID).adviceWith(camelContext, createOrUpdate());

        MockEndpoint saveUrl = camelContext.getEndpoint("mock:saveUrl", MockEndpoint.class);
        saveUrl.expectedMessageCount(1);
        camelContext.start();
        saveUrl.assertIsSatisfied();
        camelContext.stop();
    }

    private AdviceWithRouteBuilder createOrUpdate() {
        return new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById(APPEND_FEDORA_3_OBJ_INFO)
                        .replace()
                        .process(exchange -> {
                            exchange.setProperty("objectState", objState);
                            exchange.setProperty("eventType", eventType);
                            Url url = new Url();
                            url.setUrlSetUri("tud");
                            url.setLoc("https://tud/id/qucosa%3A12164");
                            url.setLastmod(DateTimeConverter.getCurrentW3cDatetime());

                            exchange.getIn().setBody(url);
                        });

                weaveByToUri(DIRECT_CREATE_URL).replace().to("mock:saveUrl");
            }
        };
    }

    private KafkaProducer<String, String> kafkaProducer() {
        Properties prodProps = new Properties();
        prodProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaCon.getBootstrapServers());
        prodProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KafkaConstants.KAFKA_DEFAULT_SERIALIZER);
        prodProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaConstants.KAFKA_DEFAULT_SERIALIZER);
        prodProps.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, KafkaConstants.KAFKA_DEFAULT_PARTITIONER);
        prodProps.put(ProducerConfig.ACKS_CONFIG, "1");
        return new KafkaProducer<String, String>(prodProps);
    }

    @SuppressWarnings("unchecked")
    private ProducerRecord<String, String> producerRecord(String data) throws JsonProcessingException {
        return (ProducerRecord<String, String>) new ProducerRecord(
                "service_events",
                0,
                "qucosa:12164",
                data);
    }

    @AfterAll
    public void shutdownAll() {
        kafkaProducer.close();
        kafkaCon.stop();
    }
}
