package de.qucosa.camel;

public class UnitTests {

//    private final Logger log = LoggerFactory.getLogger(UnitTests.class);
//    private final String FEEDER_TOPIC = "sitemap_feeder";
//    private final String MODIFY_GROUP = "modifysitemap";
//    private final String FILEPATH_INGEST = "src/test/resources/jms/?fileName=ingest.xml";
//    private final String FILEPATH_PURGEOBJECT = "src/test/resources/jms/?fileName=purgeObject.xml";
//    private final String OBJECTSTATE_INACTIVE = "I";
//    private final String OBJECTSTATE_DELETED = "D";
//    private final String OBJECTSTATE_ACTIVE = "A";
//
//    @EndpointInject(uri = "kafka:" + FEEDER_TOPIC
//        + "?groupId=" + MODIFY_GROUP
//        + "&autoOffsetReset=earliest"
//    )
//    private Endpoint fromKafka;
//
//    @EndpointInject(uri = "mock:deleted_doc")
//    private MockEndpoint docHasStateDeletedEndpoint;
//    @EndpointInject(uri = "mock:inactive_doc")
//    private MockEndpoint docHasStateInactiveEndpoint;
//    @EndpointInject(uri = "mock:ingest")
//    private MockEndpoint ingestDocEndpoint;
//    @EndpointInject(uri = "mock:purge_object")
//    private MockEndpoint purgeDocEndpoint;
//
//    @Override
//    protected RouteBuilder createRouteBuilder() {
//        return new ActiveMqRoute();
//    }
//
//    @Override
//    public boolean isUseAdviceWith() {
//        return true;
//    }
//
//    private AdviceWithRouteBuilder modifySitemapFeederRoute(String urlset, String url, String objectState) {
//        return new AdviceWithRouteBuilder() {
//            @Override
//            public void configure() throws Exception {
//                // replace Aggregationstrategy (calls Fedora for object information) by "mock"-processor
//                weaveById(APPEND_FEDORA_OBJ_INFO).replace().process((Processor) exchange -> {
//                    String jsonBodyAsString = exchange.getIn().getBody(String.class);
//                    ObjectMapper mapper = new ObjectMapper();
//
//                    JsonNode jsonBody = mapper.readTree(jsonBodyAsString);
//                    ObjectNode node = (ObjectNode) jsonBody;
//
//                    node.put("tenant_urlset", urlset);
//                    node.put("tenant_url", url);
//                    node.put("objectState", objectState);
//
//                    exchange.getIn().setBody(node.toString(), JsonObject.class);
//                });
//
//                // replace kafka-endpoint to modify kafka-behaviour for tests
//                replaceFromWith(fromKafka);
//                // replace http-endpoints by mock-endpoints
//                weaveById(HTTP_INGEST_ID).replace().to(ingestDocEndpoint);
//                weaveById(HTTP_PURGE_OBJECT_ID).replace().to(purgeDocEndpoint);
////                interceptSendToEndpoint("direct:*").to(interceptHttpRequestEndpoint);
//            }
//        };
//    }
//
//    private AdviceWithRouteBuilder mockFedoraJmsMessage(String filename) {
//        return new AdviceWithRouteBuilder() {
//            @Override
//            public void configure() {
//                // replace Fedora-AMQ-Endpoint with example file
//                replaceFromWith("file://" + filename + "&noop=true");
//            }
//        };
//    }
//
//    @Test
//    public void inactiveDocumentIsNotIngested() throws Exception {
//        context.getRouteDefinition(ACTIVEMQ_ROUTE).adviceWith(
//                context, mockFedoraJmsMessage(FILEPATH_INGEST));
//        context.getRouteDefinition(SITEMAP_FEEDER_ROUTE).adviceWith(
//                context, modifySitemapFeederRoute("testmandant", "testmandant.qucosa.de", OBJECTSTATE_INACTIVE));
//
//        docHasStateInactiveEndpoint.expectedMessageCount(1);
//        context.start();
//        docHasStateInactiveEndpoint.assertIsSatisfied();
//    }
//
//    @Test
//    public void deletedDocumentIsNotIngested() throws Exception {
//        context.getRouteDefinition(ACTIVEMQ_ROUTE).adviceWith(
//                context, mockFedoraJmsMessage(FILEPATH_INGEST));
//        context.getRouteDefinition(SITEMAP_FEEDER_ROUTE).adviceWith(
//                context, modifySitemapFeederRoute("testmandant", "testmandant.qucosa.de", OBJECTSTATE_DELETED));
//
//        docHasStateDeletedEndpoint.expectedMessageCount(1);
//        context.start();
//        docHasStateDeletedEndpoint.assertIsSatisfied();
//    }
//
//    @Test
//    public void activeDocumentIsIngested() throws Exception {
//        context.getRouteDefinition(ACTIVEMQ_ROUTE).adviceWith(
//                context, mockFedoraJmsMessage(FILEPATH_INGEST));
//        context.getRouteDefinition(SITEMAP_FEEDER_ROUTE).adviceWith(
//                context, modifySitemapFeederRoute("testmandant", "testmandant.qucosa.de", OBJECTSTATE_ACTIVE));
//
//        ingestDocEndpoint.expectedMessageCount(1);
//        context.start();
//        ingestDocEndpoint.assertIsSatisfied();
//    }
//
//    @Test
//    public void activeDocumentIsDeleted() throws Exception {
//        context.getRouteDefinition(ACTIVEMQ_ROUTE).adviceWith(
//                context, mockFedoraJmsMessage(FILEPATH_PURGEOBJECT));
//        context.getRouteDefinition(SITEMAP_FEEDER_ROUTE).adviceWith(
//                context, modifySitemapFeederRoute("testmandant", "testmandant.qucosa.de", OBJECTSTATE_ACTIVE));
//
//        purgeDocEndpoint.expectedMessageCount(1);
//        context.start();
//        purgeDocEndpoint.assertIsSatisfied();
//    }
}
