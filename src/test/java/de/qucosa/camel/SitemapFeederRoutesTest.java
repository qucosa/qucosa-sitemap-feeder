package de.qucosa.camel;

import de.qucosa.camel.model.Url;
import de.qucosa.camel.routebuilders.SitemapFeederRoutes;
import de.qucosa.camel.utils.DateTimeConverter;
import de.qucosa.data.KafkaTopicData;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.Route;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.kafka.KafkaComponent;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.Properties;

import static de.qucosa.camel.config.EndpointUris.DIRECT_CREATE_URI;
import static de.qucosa.camel.config.EndpointUris.DIRECT_DELETE_URI;
import static de.qucosa.camel.config.EndpointUris.PUSH_TO_SERVICE;
import static de.qucosa.camel.config.RouteIds.BULK_DELETE_APPEND_OBJ_INFO;
import static de.qucosa.camel.config.RouteIds.BULK_DELETE_PUSH_TO_SERVICE;
import static de.qucosa.camel.config.RouteIds.BULK_INSERT_APPEND_OBJ_INFO;
import static de.qucosa.camel.config.RouteIds.BULK_INSERT_PUSH_TO_SERVICE;
import static de.qucosa.camel.config.RouteIds.KAFKA_BULK_DELETE_ID;
import static de.qucosa.camel.config.RouteIds.KAFKA_BULK_INSERT_ID;
import static de.qucosa.camel.config.RouteIds.KAFKA_SITEMAP_CONSUMER_ID;
import static de.qucosa.camel.config.RouteIds.SITEMAP_CONSUMER_APPEND_OBJ_INFO;
import static de.qucosa.camel.config.RouteIds.SITEMAP_CONSUMER_PUSH_TO_SERVICE;

@DisplayName("Test dataflow kafka -> sitemap service.")
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
                    createContainerCmd -> createContainerCmd.withName("qucosa-sitemap-kafka"));

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

    @AfterAll
    public void shutdownAll() {
        kafkaProducer.close();
        kafkaCon.stop();
    }

    @BeforeEach
    public void setUp() throws Exception {
        camelContext.addRoutes(new SitemapFeederRoutes());

        KafkaComponent kafkaComponent = (KafkaComponent) camelContext.getComponent("kafka");
        kafkaComponent.setBrokers(kafkaCon.getBootstrapServers());
    }

    @AfterEach
    public void tearDown() throws Exception {
        for (Route route : camelContext.getRoutes()) {
            camelContext.removeRoute(route.getId());
        }
    }

    @Test
    @DisplayName("Create 'create' url object and puah to sitemap service.")
    public void pushToServiceCreate() throws Exception {
        kafkaProducer.send(producerRecord(KafkaTopicData.JSON_CREATE_EVENT));

        camelContext.getRouteDefinition(KAFKA_SITEMAP_CONSUMER_ID).adviceWith(camelContext, new AdviceWithRouteBuilder() {
            @Override
            public void configure() {
                weaveById(SITEMAP_CONSUMER_APPEND_OBJ_INFO).remove();
                weaveById(SITEMAP_CONSUMER_PUSH_TO_SERVICE).replace().to("mock:pushToService");
            }
        });

        MockEndpoint pushToService = camelContext.getEndpoint("mock:pushToService", MockEndpoint.class);
        pushToService.expectedMessageCount(1);
        camelContext.start();
        pushToService.assertIsSatisfied();
        camelContext.stop();
    }

    @Test
    @DisplayName("Create 'update' url object and puah to sitemap service.")
    public void pushToServiceUpdate() throws Exception {
        kafkaProducer.send(producerRecord(KafkaTopicData.JSON_UPDATE_EVENT));

        camelContext.getRouteDefinition(KAFKA_SITEMAP_CONSUMER_ID).adviceWith(camelContext, new AdviceWithRouteBuilder() {
            @Override
            public void configure() {
                weaveById(SITEMAP_CONSUMER_APPEND_OBJ_INFO).remove();
                weaveById(SITEMAP_CONSUMER_PUSH_TO_SERVICE).replace().to("mock:pushToService");
            }
        });

        MockEndpoint pushToService = camelContext.getEndpoint("mock:pushToService", MockEndpoint.class);
        pushToService.expectedMessageCount(1);
        camelContext.start();
        pushToService.assertIsSatisfied();
        camelContext.stop();
    }


    @Test
    @DisplayName("Create 'delete' url object and puah to sitemap service.")
    public void pushToServiceDelete() throws Exception {
        kafkaProducer.send(producerRecord(KafkaTopicData.JSON_DELETE_EVENT));

        camelContext.getRouteDefinition(KAFKA_SITEMAP_CONSUMER_ID).adviceWith(camelContext, new AdviceWithRouteBuilder() {
            @Override
            public void configure() {
                weaveById(SITEMAP_CONSUMER_APPEND_OBJ_INFO).remove();
                weaveById(SITEMAP_CONSUMER_PUSH_TO_SERVICE).replace().to("mock:pushToService");
            }
        });

        MockEndpoint pushToService = camelContext.getEndpoint("mock:pushToService", MockEndpoint.class);
        pushToService.expectedMessageCount(1);
        camelContext.start();
        pushToService.assertIsSatisfied();
        camelContext.stop();
    }

    @Test
    @DisplayName("Create the sitemap url.")
    public void createUrl() throws Exception {
        ProducerTemplate producerTemplate = camelContext.createProducerTemplate();
        camelContext.getRouteDefinition("push_service").adviceWith(camelContext, urlCUD(DIRECT_CREATE_URI,"mock:saveUrl"));
        MockEndpoint saveUrl = camelContext.getEndpoint("mock:saveUrl", MockEndpoint.class);
        saveUrl.expectedMessageCount(1);
        camelContext.start();
        producerTemplate.send(PUSH_TO_SERVICE, exchange("A", "create"));
        saveUrl.assertIsSatisfied();
        camelContext.stop();
    }

    @Test
    @DisplayName("Update the sitemap url.")
    public void update() throws Exception {
        ProducerTemplate producerTemplate = camelContext.createProducerTemplate();
        camelContext.getRouteDefinition("push_service").adviceWith(camelContext, urlCUD(DIRECT_CREATE_URI,"mock:updateUrl"));
        MockEndpoint updateUrl = camelContext.getEndpoint("mock:updateUrl", MockEndpoint.class);
        updateUrl.expectedMessageCount(1);
        camelContext.start();
        producerTemplate.send(PUSH_TO_SERVICE, exchange("A", "update"));
        updateUrl.assertIsSatisfied();
        camelContext.stop();
    }

    @Test
    @DisplayName("Delete the sitemap urls.")
    public void delete() throws Exception {
        ProducerTemplate producerTemplate = camelContext.createProducerTemplate();
        camelContext.getRouteDefinition("push_service").adviceWith(camelContext, urlCUD(DIRECT_DELETE_URI,"mock:deleteUrl"));
        MockEndpoint deleteUrl = camelContext.getEndpoint("mock:deleteUrl", MockEndpoint.class);
        deleteUrl.expectedMessageCount(1);
        camelContext.start();
        producerTemplate.send(PUSH_TO_SERVICE, exchange("A", "delete"));
        deleteUrl.assertIsSatisfied();
        camelContext.stop();
    }

    @Test
    @DisplayName("Delete url from sitemap if document hast state inactive (I).")
    public void deleteStateInactive() throws Exception {
        ProducerTemplate producerTemplate = camelContext.createProducerTemplate();
        camelContext.getRouteDefinition("push_service").adviceWith(camelContext, urlCUD(DIRECT_DELETE_URI,"mock:deleteUrl"));
        MockEndpoint deleteUrl = camelContext.getEndpoint("mock:deleteUrl", MockEndpoint.class);
        deleteUrl.expectedMessageCount(1);
        camelContext.start();
        producerTemplate.send(PUSH_TO_SERVICE, exchange("I", "delete"));
        deleteUrl.assertIsSatisfied();
        camelContext.stop();
    }

    @Test
    @DisplayName("Delete url from sitemap if document hast state delete (D).")
    public void deleteStateDelete() throws Exception {
        ProducerTemplate producerTemplate = camelContext.createProducerTemplate();
        camelContext.getRouteDefinition("push_service").adviceWith(camelContext, urlCUD(DIRECT_DELETE_URI,"mock:deleteUrl"));
        MockEndpoint deleteUrl = camelContext.getEndpoint("mock:deleteUrl", MockEndpoint.class);
        deleteUrl.expectedMessageCount(1);
        camelContext.start();
        producerTemplate.send(PUSH_TO_SERVICE, exchange("D", "delete"));
        deleteUrl.assertIsSatisfied();
        camelContext.stop();
    }

    @Test
    @DisplayName("Create url object by PID from kafka pidinsert consumer.")
    public void createFromBulkInsert() throws Exception {
        ProducerRecord<String, String> record = new ProducerRecord<>("pidupdate", 0, "qucosa:12164", "qucosa:12164");
        kafkaProducer.send(record);

        camelContext.getRouteDefinition(KAFKA_BULK_INSERT_ID).adviceWith(camelContext, new AdviceWithRouteBuilder() {
            @Override
            public void configure() {
                weaveById(BULK_INSERT_APPEND_OBJ_INFO).remove();
                weaveById(BULK_INSERT_PUSH_TO_SERVICE).replace().to("mock:pushToService");
            }
        });

        MockEndpoint pushToService = camelContext.getEndpoint("mock:pushToService", MockEndpoint.class);
        pushToService.expectedMessageCount(1);
        camelContext.start();
        pushToService.assertIsSatisfied();
        camelContext.stop();
    }

    @Test
    @DisplayName("Create url object by PID from kafka piddelete consumer.")
    public void createFromBulkDelete() throws Exception {
        ProducerRecord<String, String> record = new ProducerRecord<>("piddelete", 0, "qucosa:12164", "qucosa:12164");
        kafkaProducer.send(record);

        camelContext.getRouteDefinition(KAFKA_BULK_DELETE_ID).adviceWith(camelContext, new AdviceWithRouteBuilder() {
            @Override
            public void configure() {
                weaveById(BULK_DELETE_APPEND_OBJ_INFO).remove();
                weaveById(BULK_DELETE_PUSH_TO_SERVICE).replace().to("mock:pushToService");
            }
        });

        MockEndpoint pushToService = camelContext.getEndpoint("mock:pushToService", MockEndpoint.class);
        pushToService.expectedMessageCount(1);
        camelContext.start();
        pushToService.assertIsSatisfied();
        camelContext.stop();
    }

    private Exchange exchange(String objectState, String eventType) {
        Exchange exchange = new DefaultExchange(camelContext);

        exchange.setProperty("objectState", objectState);
        exchange.setProperty("eventType", eventType);
        exchange.setProperty("pid", "qucosa:12164");

        Url url = new Url();
        url.setUrlSetUri("tud");
        url.setLoc("https://tud/id/qucosa%3A12164");
        url.setLastmod(DateTimeConverter.getCurrentW3cDatetime());

        exchange.getIn().setBody(url);

        return exchange;
    }

    private AdviceWithRouteBuilder urlCUD(String sitemapUri, String mock) {
        return new AdviceWithRouteBuilder() {
            @Override
            public void configure() {
                weaveByToUri(sitemapUri).replace().to(mock);
            }
        };
    }

    private AdviceWithRouteBuilder bulkInsertBuilder() {
        return new AdviceWithRouteBuilder() {
            @Override
            public void configure() {
                weaveById(BULK_INSERT_APPEND_OBJ_INFO).remove();
                weaveById(BULK_INSERT_PUSH_TO_SERVICE).replace().to("mock:bulkInsert");
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
        return new KafkaProducer<>(prodProps);
    }

    @SuppressWarnings("unchecked")
    private ProducerRecord<String, String> producerRecord(String data) {
        return (ProducerRecord<String, String>) new ProducerRecord(
                "service_events",
                0,
                "qucosa:12164",
                data);
    }
}
