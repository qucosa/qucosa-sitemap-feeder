package de.qucosa.camel;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.qucosa.camel.model.Url;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Properties;

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
        kafkaCon.execInContainer("/bin/sh", "-c", "kafka-topics --zookeeper localhost:2181 --partitions=1 --replication-factor=1 --create --topic pidupdate");
        kafkaCon.execInContainer("/bin/sh", "-c", "kafka-topics --zookeeper localhost:2181 --partitions=1 --replication-factor=1 --create --topic piddelete");
        kafkaCon.execInContainer("/bin/sh", "-c", "kafka-topics --zookeeper localhost:2181 --partitions=1 --replication-factor=1 --create --topic service_events");

        PropertiesComponent pc = (PropertiesComponent) camelContext.getComponent("properties");
        pc.setLocation("classpath:application-test.properties");
        camelContext.addRoutes(new SitemapFeederRoutes());

        KafkaComponent kafkaComponent = (KafkaComponent) camelContext.getComponent("kafka");
        kafkaComponent.setBrokers(kafkaCon.getBootstrapServers());

        camelContext.getRouteDefinition(SITEMAP_FEEDER_ROUTE).adviceWith(camelContext, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveById(APPEND_FEDORA_OBJ_INFO)
                        .replace()
                        .process(exchange -> {
                            exchange.setProperty("objectState", "A");
                            exchange.setProperty("eventType", "create");
                            Url url = new Url();
                            url.setUrlSetUri("tud");
                            url.setLoc("https://tud/id/qucosa%3A12164");
                            url.setLastmod(DateTimeConverter.getCurrentW3cDatetime());

                            exchange.getIn().setBody(url);
                        });

                weaveByToUri("direct:sitemap_create_url").replace().to("mock:ingest");
            }
        });
    }

    @Test
    @DisplayName("Ingest the sitemap url.")
    public void ingest() throws Exception {
        KafkaProducer<String, String> kafkaProducer = kafkaProducer();
        kafkaProducer.send(producerRecord());
        kafkaProducer.close();

        MockEndpoint ingest = camelContext.getEndpoint("mock:ingest", MockEndpoint.class);
        ingest.expectedMessageCount(1);
        camelContext.start();
        ingest.assertIsSatisfied();
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
    private ProducerRecord<String, String> producerRecord() throws JsonProcessingException {
        return (ProducerRecord<String, String>) new ProducerRecord(
                "service_events",
                0,
                "qucosa:12164",
                KafkaTopicData.JSON_CREATE_EVENT);
    }

    @AfterAll
    public void shutdown() {
        kafkaCon.stop();
    }
}
