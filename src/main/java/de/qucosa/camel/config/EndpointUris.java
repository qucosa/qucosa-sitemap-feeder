package de.qucosa.camel.config;

public class EndpointUris {
    public final static String KAFKA_SITEMAP_CONSUMER = "kafka:service_events?groupId=sitemap&consumersCount=1&breakOnFirstError=true&autoOffsetReset=earliest";
    public final static String FEDORA_3_OBJECTINFO = "seda:objectinfo?multipleConsumers=true";
    public final static String DIRECT_CREATE_URL = "direct:sitemap_create_url";
    public final static String SITEMAP_SERVICE_CREATE_URL = "http4://{{sitemap.service.url}}/url";
}
