package de.qucosa.camel.config;

public class EndpointUris {
    public final static String KAFKA_SITEMAP_CONSUMER = "kafka:service_events?groupId=sitemap&consumersCount=1&breakOnFirstError=true&autoOffsetReset=earliest";
    public final static String KAFKA_BULK_INSERT_CONSUMER = "kafka:pidupdate?groupId=bulkinsert&consumersCount=1&breakOnFirstError=true&autoOffsetReset=earliest";
    public final static String KAFKA_BULK_DELETE_CONSUMER = "kafka:piddelete?groupId=bulkdelete&consumersCount=1&breakOnFirstError=true&autoOffsetReset=earliest";
    public final static String FEDORA_3_OBJECTINFO = "direct:objectinfo";
    public final static String DIRECT_CREATE_URI = "direct:sitemap_create_url";
    public final static String DIRECT_DELETE_URI = "direct:sitemap_delete_url";
    public final static String PUSH_TO_SERVICE = "direct:push_to_service";
    public final static String SITEMAP_SERVICE_URI = "http4://{{sitemap.service.url}}/url";
}
