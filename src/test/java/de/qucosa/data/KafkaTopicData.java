package de.qucosa.data;

public class KafkaTopicData {
    public static final String JSON_DELETE_EVENT =
            "{\"org.fcrepo.jms.ownerID\":\"tud\"," +
            "{\"org.fcrepo.jms.objectState\":\"A\"," +
            "{\"org.fcrepo.jms.user\":\"sword\"," +
            "\"org.fcrepo.jms.eventID\":\"urn:uuid:0b56626a-7b38-429a-86d6-6ebebe310287\"," +
            "\"org.fcrepo.jms.identifier\":\"qucosa:12164\"," +
            "\"org.fcrepo.jms.timestamp\":1544089905392," +
            "\"org.fcrepo.jms.baseURL\":\"http://sdvcmr-app03:8080/fedora\"," +
            "\"org.fcrepo.jms.eventType\":\"delete\"," +
            "\"org.fcrepo.jms.resourceType\":\"http://www.w3.org/ns/ldp#Container, http://fedora.info/definitions/v4/repository#Resource\"," +
            "\"org.fcrepo.jms.userAgent\":\"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.117 Safari/537.36\"}";

    public static final String JSON_CREATE_EVENT =
            "{\"org.fcrepo.jms.ownerID\":\"tud\"," +
            "{\"org.fcrepo.jms.objectState\":\"A\"," +
            "{\"org.fcrepo.jms.user\":\"sword\"," +
            "\"org.fcrepo.jms.eventID\":\"urn:uuid:0b56626a-7b38-429a-86d6-6ebebe310287\"," +
            "\"org.fcrepo.jms.identifier\":\"qucosa:12164\"," +
            "\"org.fcrepo.jms.timestamp\":1544089905392," +
            "\"org.fcrepo.jms.baseURL\":\"http://sdvcmr-app03:8080/fedora\"," +
            "\"org.fcrepo.jms.eventType\":\"create\"," +
            "\"org.fcrepo.jms.resourceType\":\"http://www.w3.org/ns/ldp#Container, http://fedora.info/definitions/v4/repository#Resource\"," +
            "\"org.fcrepo.jms.userAgent\":\"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.117 Safari/537.36\"}";

    public static final String JSON_UPDATE_EVENT =
            "{\"org.fcrepo.jms.ownerID\":\"tud\"," +
            "{\"org.fcrepo.jms.objectState\":\"A\"," +
            "{\"org.fcrepo.jms.user\":\"sword\"," +
            "\"org.fcrepo.jms.eventID\":\"urn:uuid:0b56626a-7b38-429a-86d6-6ebebe310287\"," +
            "\"org.fcrepo.jms.identifier\":\"qucosa:12164\"," +
            "\"org.fcrepo.jms.timestamp\":1544089905392," +
            "\"org.fcrepo.jms.baseURL\":\"http://sdvcmr-app03:8080/fedora\"," +
            "\"org.fcrepo.jms.eventType\":\"update\"," +
            "\"org.fcrepo.jms.resourceType\":\"http://www.w3.org/ns/ldp#Container, http://fedora.info/definitions/v4/repository#Resource\"," +
            "\"org.fcrepo.jms.userAgent\":\"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.117 Safari/537.36\"}";
}
