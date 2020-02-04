package de.qucosa.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.qucosa.camel.model.Tenant;
import de.qucosa.camel.model.Url;
import de.qucosa.camel.utils.DocumentXmlUtils;
import de.qucosa.data.KafkaTopicData;
import de.qucosa.events.FedoraUpdateEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Test dataflow activeMQ -> kafka -> sitemap service.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UrlObjectBuilderTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Object state is fedora 3 only
    @Test
    @DisplayName("Test if object state active.")
    public void objectState() throws IOException {
        UrlObjectBuilder urlObjectBuilder = urlObjectBuilder();

        assertEquals(urlObjectBuilder.objectState(), "A");
    }

    @Test
    @DisplayName("Test if sitemap url object complete and corect.")
    public void sitemapUrlObject() throws Exception {
        Url url = urlObjectBuilder().sitemapUrlObject();
        assertEquals(url.getUrlSetUri(), "tud");
        assertEquals(url.getLoc(), "https://tud/id/qucosa%3A12164");
    }

    private UrlObjectBuilder urlObjectBuilder() throws IOException {

        return new UrlObjectBuilder(
            objectMapper.readValue(KafkaTopicData.JSON_CREATE_EVENT, FedoraUpdateEvent.class),
            DocumentXmlUtils.document(getClass().getResourceAsStream("/fedora/qucosa:12164.xml"), true),
            objectMapper.readValue(getClass().getResourceAsStream("/config/tenant.json"), objectMapper.getTypeFactory().constructCollectionType(List.class, Tenant.class)));
    }
}
